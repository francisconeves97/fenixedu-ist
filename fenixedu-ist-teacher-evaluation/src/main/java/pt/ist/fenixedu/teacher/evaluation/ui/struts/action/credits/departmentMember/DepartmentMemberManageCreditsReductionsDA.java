/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST Teacher Evaluation.
 *
 * FenixEdu IST Teacher Evaluation is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST Teacher Evaluation is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST Teacher Evaluation.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.teacher.evaluation.ui.struts.action.credits.departmentMember;

import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.fenixedu.academic.domain.Department;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Teacher;
import org.fenixedu.academic.service.services.exceptions.FenixServiceException;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.bennu.struts.annotations.Forward;
import org.fenixedu.bennu.struts.annotations.Forwards;
import org.fenixedu.bennu.struts.annotations.Mapping;
import org.fenixedu.bennu.struts.portal.EntryPoint;
import org.fenixedu.bennu.struts.portal.StrutsFunctionality;

import pt.ist.fenixWebFramework.renderers.utils.RenderUtils;
import pt.ist.fenixedu.contracts.domain.accessControl.DepartmentPresidentStrategy;
import pt.ist.fenixedu.teacher.evaluation.domain.credits.AnnualCreditsState;
import pt.ist.fenixedu.teacher.evaluation.domain.credits.util.ReductionServiceBean;
import pt.ist.fenixedu.teacher.evaluation.domain.teacher.ReductionService;
import pt.ist.fenixedu.teacher.evaluation.domain.teacher.TeacherService;
import pt.ist.fenixedu.teacher.evaluation.ui.struts.action.credits.ManageCreditsReductionsDispatchAction;
import pt.ist.fenixedu.teacher.evaluation.ui.struts.action.departmentMember.DepartmentMemberPresidentApp;
import pt.ist.fenixframework.FenixFramework;

@StrutsFunctionality(app = DepartmentMemberPresidentApp.class, path = "credits-reductions",
        titleKey = "label.credits.creditsReduction", bundle = "TeacherCreditsSheetResources")
@Mapping(module = "departmentMember", path = "/creditsReductions")
@Forwards(value = {
        @Forward(name = "editReductionService",
                path = "/teacher/evaluation/credits/degreeTeachingService/editCreditsReduction.jsp"),
        @Forward(name = "viewAnnualTeachingCredits", path = "/departmentMember/credits.do?method=viewAnnualTeachingCredits"),
        @Forward(name = "showReductionServices", path = "/teacher/evaluation/credits/reductionService/showReductionServices.jsp"),
        @Forward(name = "showReductionService", path = "/teacher/evaluation/credits/reductionService/showReductionService.jsp") })
public class DepartmentMemberManageCreditsReductionsDA extends ManageCreditsReductionsDispatchAction {

    @EntryPoint
    public ActionForward showReductionServices(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws NumberFormatException, FenixServiceException {
        ExecutionYear executionYear = AnnualCreditsState.getExecutionYearForValidReductionServiceApprovalPeriod();
        if (executionYear == null) {
            executionYear = ExecutionYear.readCurrentExecutionYear();
        }
        User userView = Authenticate.getUser();
        Department department = userView.getPerson().getTeacher().getDepartment();
        SortedSet<ReductionService> creditsReductions = new TreeSet<ReductionService>(new Comparator<ReductionService>() {
            @Override
            public int compare(ReductionService reductionService1, ReductionService reductionService2) {
                final int teacherIdCompare = reductionService1.getTeacherService().getTeacher().getPerson().getUsername()
                        .compareTo(reductionService2.getTeacherService().getTeacher().getPerson().getUsername());
                return teacherIdCompare == 0 ? reductionService1.getTeacherService().getExecutionPeriod()
                        .compareExecutionInterval(reductionService2.getTeacherService().getExecutionPeriod()) : teacherIdCompare;
            }
        });

        boolean isInValidReductionServiceApprovalPeriod =
                AnnualCreditsState.isInValidReductionServiceApprovalPeriod(executionYear);
        if (department != null && DepartmentPresidentStrategy.isCurrentUserCurrentDepartmentPresident(department)
                && isInValidReductionServiceApprovalPeriod) {

            List<Teacher> allTeachers = department.getAllTeachers(executionYear);

            for (ExecutionSemester executionSemester : executionYear.getExecutionPeriodsSet()) {
                boolean inValidTeacherCreditsPeriod = AnnualCreditsState.isInValidCreditsPeriod(executionSemester);

                for (Teacher teacher : allTeachers) {
                    TeacherService teacherService = TeacherService.getTeacherServiceByExecutionPeriod(teacher, executionSemester);
                    if (teacherService != null && teacherService.getReductionService() != null
                            && ((teacherService.getReductionService().getRequestCreditsReduction() != null
                                    && teacherService.getReductionService().getRequestCreditsReduction())
                                    || teacherService.getReductionService().getCreditsReductionAttributed() != null)
                            && (teacherService.getTeacherServiceLock() != null || !inValidTeacherCreditsPeriod)) {
                        creditsReductions.add(teacherService.getReductionService());
                    }
                }
            }
        }
        request.setAttribute("executionYear", executionYear);
        request.setAttribute("creditsReductions", creditsReductions);
        return mapping.findForward("showReductionServices");
    }

    public ActionForward aproveReductionService(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws NumberFormatException, FenixServiceException {
        ReductionService reductionService =
                FenixFramework.getDomainObject((String) getFromRequest(request, "reductionServiceOID"));
        ReductionServiceBean reductionServiceBean = null;
        if (reductionService != null) {
            reductionServiceBean = new ReductionServiceBean(reductionService);
        } else {
            reductionServiceBean = getRenderedObject("reductionServiceBean");
            if (reductionServiceBean != null && reductionServiceBean.getTeacher() != null) {
                if (reductionServiceBean.getReductionService() == null) {
                    TeacherService teacherService = TeacherService.getTeacherServiceByExecutionPeriod(
                            reductionServiceBean.getTeacher(), reductionServiceBean.getExecutionSemester());
                    if (teacherService != null) {
                        reductionServiceBean.setReductionService(teacherService.getReductionService());
                    }
                }
            }
        }
        if (request.getParameter("invalidated") == null) {
            RenderUtils.invalidateViewState();
        }
        request.setAttribute("reductionServiceBean", reductionServiceBean);
        return mapping.findForward("showReductionService");
    }

    public ActionForward selectTeacher(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws NumberFormatException, FenixServiceException {
        request.setAttribute("reductionServiceBean", new ReductionServiceBean());
        return mapping.findForward("showReductionService");
    }
}
