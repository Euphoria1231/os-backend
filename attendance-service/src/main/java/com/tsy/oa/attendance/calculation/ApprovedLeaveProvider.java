package com.tsy.oa.attendance.calculation;

import java.time.LocalDate;
import java.util.List;

public interface ApprovedLeaveProvider {

    List<ApprovedLeave> findApprovedLeaves(LocalDate date);
}
