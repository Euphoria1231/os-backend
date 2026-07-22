package com.tsy.oa.gateway;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class SeedDataMigrationTests {

    private static final Pattern ROLE_PERMISSION_DELETE = Pattern.compile(
            "DELETE\\s+FROM\\s+role_api_permission\\s+"
                    + "WHERE\\s+role_id\\s*=\\s*2\\s+"
                    + "AND\\s+api_permission_id\\s+IN\\s*\\(\\s*9\\s*,\\s*10\\s*\\)\\s*;",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    private static final Pattern ANY_ROLE_PERMISSION_DELETE = Pattern.compile(
            "DELETE\\s+FROM\\s+role_api_permission\\b.*?;",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    private static final Pattern ROLE_PERMISSION_INSERT = Pattern.compile(
            "INSERT\\s+INTO\\s+role_api_permission\\s*\\([^)]*\\)\\s*"
                    + "VALUES\\s*(.*?)\\s+AS\\s+incoming\\s+ON\\s+DUPLICATE\\s+KEY\\s+UPDATE",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    private static final Pattern PERMISSION_PAIR = Pattern.compile("\\((\\d+)\\s*,\\s*(\\d+)\\)");

    @Test
    void rolePermissionSeedRemovesLegacyEmployeeTaskPermissionsIdempotently() throws IOException {
        Path repositoryRoot = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        Path seedScript = repositoryRoot.resolve("sql/init_data.sql");
        if (!Files.exists(seedScript)) {
            seedScript = repositoryRoot.resolve("../sql/init_data.sql").normalize();
        }
        String sql = Files.readString(seedScript, StandardCharsets.UTF_8);
        List<String> rolePermissionDeletes = findMatches(ANY_ROLE_PERMISSION_DELETE, sql);

        assertThat(rolePermissionDeletes).singleElement().satisfies(statement ->
                assertThat(statement).matches(ROLE_PERMISSION_DELETE));

        Set<RolePermission> permissions = new HashSet<>(Set.of(
                new RolePermission(2, 5),
                new RolePermission(2, 9),
                new RolePermission(2, 10),
                new RolePermission(2, 99),
                new RolePermission(3, 9)
        ));

        applyRolePermissionSeed(sql, permissions);
        assertThat(permissions)
                .doesNotContain(new RolePermission(2, 9), new RolePermission(2, 10))
                .contains(new RolePermission(2, 5), new RolePermission(2, 99), new RolePermission(3, 9));

        Set<RolePermission> firstRun = Set.copyOf(permissions);
        applyRolePermissionSeed(sql, permissions);
        assertThat(permissions).isEqualTo(firstRun);
    }

    private List<String> findMatches(Pattern pattern, String sql) {
        List<String> matches = new ArrayList<>();
        Matcher matcher = pattern.matcher(sql);
        while (matcher.find()) {
            matches.add(matcher.group());
        }
        return matches;
    }

    private void applyRolePermissionSeed(String sql, Set<RolePermission> permissions) {
        if (ROLE_PERMISSION_DELETE.matcher(sql).find()) {
            permissions.remove(new RolePermission(2, 9));
            permissions.remove(new RolePermission(2, 10));
        }

        Matcher insert = ROLE_PERMISSION_INSERT.matcher(sql);
        assertThat(insert.find()).isTrue();
        Matcher pair = PERMISSION_PAIR.matcher(insert.group(1));
        while (pair.find()) {
            permissions.add(new RolePermission(
                    Integer.parseInt(pair.group(1)),
                    Integer.parseInt(pair.group(2))
            ));
        }
    }

    private record RolePermission(int roleId, int apiPermissionId) {
    }
}
