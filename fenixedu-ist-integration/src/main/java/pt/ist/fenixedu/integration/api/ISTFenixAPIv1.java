/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST Integration.
 *
 * FenixEdu IST Integration is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST Integration is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST Integration.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.integration.api;

import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.fenixedu.academic.api.FenixAPIv1;
import org.fenixedu.academic.api.beans.FenixPerson;
import org.fenixedu.academic.api.dto.PersonInformationBean;
import org.fenixedu.academic.domain.*;
import org.fenixedu.academic.domain.accessControl.ActiveStudentsGroup;
import org.fenixedu.academic.domain.accessControl.ActiveTeachersGroup;
import org.fenixedu.academic.domain.accessControl.AllAlumniGroup;
import org.fenixedu.academic.domain.degreeStructure.CourseGroup;
import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicInterval;
import org.fenixedu.bennu.core.domain.Avatar;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.bennu.oauth.annotation.OAuthEndpoint;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.YearMonthDay;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import pt.ist.fenixedu.contracts.domain.Employee;
import pt.ist.fenixedu.contracts.domain.accessControl.ActiveEmployees;
import pt.ist.fenixedu.contracts.domain.organizationalStructure.Contract;
import pt.ist.fenixedu.contracts.domain.personnelSection.contracts.PersonContractSituation;
import pt.ist.fenixedu.contracts.domain.personnelSection.contracts.PersonProfessionalData;
import pt.ist.fenixedu.contracts.domain.personnelSection.contracts.ProfessionalCategory;
import pt.ist.fenixedu.contracts.domain.util.CategoryType;
import pt.ist.fenixedu.integration.FenixEduIstIntegrationConfiguration;
import pt.ist.fenixedu.integration.api.beans.publico.FenixDepartment;
import pt.ist.fenixedu.integration.api.infra.FenixAPIFromExternalServer;
import pt.ist.fenixedu.integration.service.services.externalServices.CreatePreEnrolment;
import pt.ist.fenixframework.DomainObject;
import pt.ist.fenixframework.FenixFramework;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Path("/fenix/v1")
public class ISTFenixAPIv1 extends FenixAPIv1 {

    public final static String PERSONAL_SCOPE = "INFO";
    public final static String DEGREE_CURRICULAR_MANAGEMENT = "DEGREE_CURRICULAR_MANAGEMENT";

    public final static String JSON_UTF8 = "application/json; charset=utf-8";

    public final static String dayPattern = "dd/MM/yyyy";
    public final static String dayHourPattern = "dd/MM/yyyy HH:mm";

    public static final DateTimeFormatter formatDay = DateTimeFormat.forPattern(dayPattern);

    private final <T extends DomainObject> T getDomainObject(final String externalId, final Class<T> clazz) {
        try {
            T domainObject = FenixFramework.getDomainObject(externalId);
            if (!FenixFramework.isDomainObjectValid(domainObject) || !clazz.isAssignableFrom(domainObject.getClass())) {
                throw newApplicationError(Status.NOT_FOUND, "id not found", "id not found");
            }
            return domainObject;
        } catch (Exception nfe) {
            throw newApplicationError(Status.NOT_FOUND, "id not found", "id not found");
        }
    }

    private WebApplicationException newApplicationError(Status status, String error, String description) {
        JsonObject errorObject = new JsonObject();
        errorObject.addProperty("error", error);
        errorObject.addProperty("description", description);
        return new WebApplicationException(Response.status(status).entity(errorObject.toString()).build());
    }

    @Override
    protected Set<FenixPerson.FenixRole> getPersonRoles(Person person, PersonInformationBean pib) {
        User user = person.getUser();

        final Set<FenixPerson.FenixRole> roles = new HashSet<>();

        if (new ActiveTeachersGroup().isMember(user)) {
            roles.add(new FenixPerson.TeacherFenixRole(pib.getTeacherDepartment()));
        }

        if (new ActiveStudentsGroup().isMember(user)) {
            roles.add(new FenixPerson.StudentFenixRole(pib.getStudentRegistrations()));
        }

        if (new AllAlumniGroup().isMember(user)) {
            ArrayList<Registration> concludedRegistrations = new ArrayList<>();
            if (person.getStudent() != null) {
                concludedRegistrations.addAll(person.getStudent().getConcludedRegistrations());
            }
            roles.add(new FenixPerson.AlumniFenixRole(concludedRegistrations));
        }

        if (new ActiveEmployees().isMember(user)) {
            roles.add(new FenixPerson.EmployeeFenixRole());
        }

        return roles;
    }

    /**
     * Returns announcements for course by id
     *

     * @param oid
     *            course id
     * @return
     */
    @GET
    @Produces(JSON_UTF8)
    @Path("courses/{id}/announcements")
    public List<FenixCourseAnnouncement> courseAnnouncementsByOid(@PathParam("id") String oid) {
        final ExecutionCourse executionCourse = getDomainObject(oid, ExecutionCourse.class);

        List<FenixCourseAnnouncement> announcements = executionCourse.getSite().getCategoriesSet().stream()
                .filter(category -> category.getSlug().equals("announcement"))
                .flatMap(category -> category.getPostsSet().stream())
                .filter(post -> post.isVisible() && post.getCanViewGroup().isMember(null))
                .map(FenixCourseAnnouncement::new)
                .collect(Collectors.toList());

        return announcements;
    }

    @GET
    @Produces(JSON_UTF8)
    @Path("canteen")
    public String canteen(@QueryParam("day") String day, @QueryParam("name") String canteenName) {
        validateDay(day);
        if (day == null || Strings.isNullOrEmpty(day.trim())) {
            day = formatDay.print(new DateTime());
        }
        if (canteenName == null || Strings.isNullOrEmpty(canteenName.trim())) {
            canteenName = FenixEduIstIntegrationConfiguration.getConfiguration().getFenixAPICanteenDefaultName();
        }
        return FenixAPIFromExternalServer.getCanteen(day, canteenName);
    }

    @GET
    @Produces(JSON_UTF8)
    @Path("contacts")
    public String contacts() {
        return FenixAPIFromExternalServer.getContacts();
    }

    @GET
    @Produces(JSON_UTF8)
    @Path("shuttle")
    public String shuttle() {
        return FenixAPIFromExternalServer.getShuttle();
    }

    private static Unit getSectionOrScientificArea(Unit unit) {
        if (unit == null || unit.isScientificAreaUnit() || unit.isSectionUnit()) {
            return unit;
        }
        return unit.getParentUnits().stream()
                .map(ISTFenixAPIv1::getSectionOrScientificArea)
                .filter(Objects::nonNull).findFirst().orElse(null);
    }

    private static String localizedName(CategoryType type) {
        return BundleUtil.getString("resources.FenixeduIstIntegrationResources", "label.contract.category.type." + type.getName());
    }

    private static Duration getDuration(PersonContractSituation s) {
        return s.getEndDate() != null ? new Duration(s.getBeginDate().toDateTimeAtStartOfDay(), s.getEndDate().toDateTimeAtStartOfDay()) : null;
    }

    private static Duration getDuration(Contract c) {
        return c.getEndDate() != null ? new Duration(c.getBeginDate().toDateMidnight(), c.getEndDate().toDateMidnight()) : null;
    }

    private static Duration getDuration(TeacherAuthorization ta) {
        ExecutionSemester semester = ta.getExecutionSemester();
        return new Duration(semester.getBeginLocalDate().toDateTimeAtCurrentTime(), semester.getEndLocalDate().toDateTimeAtCurrentTime());
    }

    private static boolean overlaps(TeacherAuthorization ta, AcademicInterval interval) {
        return ta.getExecutionSemester().getAcademicInterval().overlaps(interval);
    }

    private static boolean overlaps(PersonContractSituation situation, AcademicInterval interval) {
        try {
            return situation.getBeginDate() != null && situation.overlaps(interval.toInterval());
        } catch (Exception e) { //XXX erroneous contract dates
            return false;
        }
    }

    private static <T, C> Optional<C> getLongestLasting(Function<T, C> get, Stream<T> stream, Function<T, Duration> getDuration) {
        return stream.filter(i->get.apply(i)!=null).collect(Collectors.groupingBy(get, Collectors.reducing(Duration.ZERO, getDuration, Duration::plus)))
                .entrySet().stream().max(Comparator.comparing(Map.Entry::getValue)).map(Map.Entry::getKey);
    }

    private static Unit getPersonDepartmentArea(Employee employee, AcademicInterval interval, boolean direct, Unit defaultArea) {
        if (employee != null) {
            Stream<Contract> contracts = employee.getWorkingContracts(interval.getBeginYearMonthDayWithoutChronology(), interval.getEndYearMonthDayWithoutChronology()).stream();
            Optional<Unit> workingUnit = getLongestLasting(Contract::getWorkingUnit, contracts, ISTFenixAPIv1::getDuration);
            if (!direct) {
                workingUnit = workingUnit.map(ISTFenixAPIv1::getSectionOrScientificArea);
            }
            if (workingUnit.isPresent()) {
                return workingUnit.get();
            }
        }
        return defaultArea;
    }

    private static FenixDepartment.FenixDepartmentMember getFenixDepartmentMember(Person person) {
        String username = person.getUsername();
        String name = person.getDisplayName();
        String email = person.getEmailForSendingEmails();
        String photo = Avatar.mysteryManUrl(person.getUser());
        if (person.getProfile() != null) {
            photo = person.getProfile().getAvatarUrl();
        }

        return new FenixDepartment.FenixDepartmentMember(username, name, email, photo);
    }

    private static FenixDepartment.FenixDepartmentMember getFenixDepartmentTeacher(Department department, Person person, AcademicInterval interval) {
        FenixDepartment.FenixDepartmentMember member = getFenixDepartmentMember(person);

        Stream<TeacherAuthorization> authorizations = person.getTeacher().getTeacherAuthorizationStream().filter(ta -> overlaps(ta, interval));
        TeacherCategory category = getLongestLasting(TeacherAuthorization::getTeacherCategory, authorizations, ISTFenixAPIv1::getDuration).get();

        member.setRole(localizedName(CategoryType.TEACHER));
        member.setCategory(category.getName().getContent());
        member.setArea(getPersonDepartmentArea(person.getEmployee(), interval, false, department.getDepartmentUnit()).getNameI18n().getContent());

        return member;
    }

    private static FenixDepartment.FenixDepartmentMember getFenixDepartmentEmployee(Department department, Person person, AcademicInterval interval) {
        FenixDepartment.FenixDepartmentMember member = getFenixDepartmentMember(person);
        YearMonthDay begin = interval.getBeginYearMonthDayWithoutChronology();
        YearMonthDay end = interval.getEndYearMonthDayWithoutChronology();

        CategoryType type = CategoryType.EMPLOYEE;
        String category = "";
        Optional<ProfessionalCategory> data = Optional.ofNullable(person.getPersonProfessionalData())
                .map(PersonProfessionalData::getGiafProfessionalData)
                .flatMap(gpd -> getLongestLasting(PersonContractSituation::getProfessionalCategory, gpd.getPersonContractSituationsSet().stream().filter(s -> overlaps(s, interval)), ISTFenixAPIv1::getDuration));
        if (data.isPresent()) {
            ProfessionalCategory pcat = data.get();
            type = pcat.getCategoryType();
            category = pcat.getName().getContent();
        }
        String role = localizedName(type);
        boolean direct = type.equals(CategoryType.EMPLOYEE) || type.equals(CategoryType.GRANT_OWNER);
        String area = getPersonDepartmentArea(person.getEmployee(), interval, direct, department.getDepartmentUnit()).getNameI18n().getContent();

        member.setRole(role);
        member.setCategory(category);
        member.setArea(area);

        return member;
    }

    private static FenixDepartment getFenixDepartment(Department department, AcademicInterval interval) {
        String name = department.getNameI18n().getContent();
        String acronym = department.getAcronym();

        List<Employee> employees = Employee.getAllWorkingEmployees(department, interval.getBeginYearMonthDayWithoutChronology(), interval.getEndYearMonthDayWithoutChronology());
        Set<Person> employeePeople = employees.stream().map(Employee::getPerson).collect(Collectors.toSet());
        //XXX Teachers are obtained separately through their TeacherAuthorizations so external teachers are taken into account
        //XXX Internal teachers with missing authorizations are present in the employee stream and are displayed in the same way as others
        List<Teacher> teachers = department.getAllTeachers(interval);
        Set<Person> teacherPeople = teachers.stream().map(Teacher::getPerson).collect(Collectors.toSet());
        employeePeople.removeAll(teacherPeople);

        List<FenixDepartment.FenixDepartmentMember> members = new ArrayList<>();
        teacherPeople.stream().map(p -> getFenixDepartmentTeacher(department, p, interval)).forEach(members::add);
        employeePeople.stream().map(p -> getFenixDepartmentEmployee(department, p, interval)).forEach(members::add);

        return new FenixDepartment(name, acronym, members);
    }

    @GET
    @Produces(JSON_UTF8)
    @Path("departments/{acronym}")
    public FenixDepartment departmentByAcronym(@PathParam("acronym") String acronym, @QueryParam("academicTerm") String academicTerm) {
        final AcademicInterval interval = getAcademicInterval(academicTerm);
        Optional<Department> department = Bennu.getInstance().getDepartmentsSet().stream().filter(d -> d.getAcronym().equals(acronym)).findFirst();
        if (department.isPresent()) {
            return getFenixDepartment(department.get(), interval);
        } else {
            throw newApplicationError(Status.NOT_FOUND, "department not found", "department not found");
        }
    }

    /**
     * Set the previously chosen courses for students
     *

     * @param oid degree id
     * @param academicTerm
     * @param preEnrolments
     * 
     */
    @POST
    @Produces(JSON_UTF8)
    @Path("degrees/{id}/preEnrolmentsCurricularGroups")
    @OAuthEndpoint(value = DEGREE_CURRICULAR_MANAGEMENT)
    public Response defaultEnrolments(@PathParam("id") String oid, @QueryParam("academicTerm") String academicTerm,
            JsonObject preEnrolments) {

        AcademicInterval academicInterval = getAcademicInterval(academicTerm, true);
        if (academicInterval == null) {
            throw newApplicationError(Status.BAD_REQUEST, "format_error", "academicTerm parameter must not be null");
        }

        ExecutionInterval executionInterval = ExecutionInterval.getExecutionInterval(academicInterval);
        if (!(executionInterval instanceof ExecutionSemester)) {
            throw newApplicationError(Status.BAD_REQUEST, "format_error", "academicTerm parameter must be a semester");
        }
        ExecutionSemester executionSemester = (ExecutionSemester) executionInterval;

        Degree degree = getDomainObject(oid, Degree.class);
        final Person coordinator = Authenticate.getUser().getPerson();
        boolean isCoordinator =
                degree.getResponsibleCoordinators(executionSemester.getExecutionYear()).stream().map(Coordinator::getPerson)
                        .anyMatch(p -> p == coordinator);
        if (!isCoordinator) {
            throw newApplicationError(Status.BAD_REQUEST, "notAllowed",
                    "must be a responsible coordinator of the degree in the given semester");
        }

        JsonObject report = new JsonObject();
        report.add("errors", new JsonArray());

        JsonArray jArray = preEnrolments.getAsJsonArray("defaultEnrolments");
        for (JsonElement jsonElement : jArray) {
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            String userId = jsonObject.get("student").getAsString();
            User user = User.findByUsername(userId);
            if (user == null) {
                report.get("errors").getAsJsonArray()
                        .add(new JsonPrimitive("User " + userId + " not found. Pre enrolments for this user were not processed"));
                break;
            }
            for (JsonElement jsonPreEnrol : jsonObject.get("preEnrolments").getAsJsonArray()) {
                JsonObject jsonEnrolLine = jsonPreEnrol.getAsJsonObject();
                String curricularCourseId = jsonEnrolLine.get("course").getAsString();
                String groupId = jsonEnrolLine.get("group").getAsString();

                CurricularCourse curricularCourse = null;
                try {
                    curricularCourse = FenixFramework.getDomainObject(curricularCourseId);
                } catch (Exception e) {
                    report.get("errors").getAsJsonArray()
                            .add(new JsonPrimitive("Curricular course " + curricularCourseId + " not found for user " + userId));
                    break;
                }
                CourseGroup courseGroup = null;
                try {
                    courseGroup = FenixFramework.getDomainObject(groupId);
                } catch (Exception e) {
                    report.get("errors").getAsJsonArray()
                            .add(new JsonPrimitive("Course group " + groupId + " not found for user " + userId));
                    break;
                }

                try {
                    CreatePreEnrolment.create(executionSemester, degree, user, curricularCourse, courseGroup);
                } catch (Exception e) {
                    report.get("errors")
                            .getAsJsonArray()
                            .add(new JsonPrimitive("An error occured for user " + userId + " course group " + groupId
                                    + " and curricular course " + curricularCourseId
                                    + " - this pre enrolment was not successful - Exception: " + e.getMessage()));
                }
            }
        }
        return Response.ok(report).build();
    }

    private AcademicInterval getDefaultAcademicTerm() {
        return ExecutionSemester.readActualExecutionSemester().getAcademicInterval();
    }

    private AcademicInterval getAcademicInterval(String academicTerm) {
        return getAcademicInterval(academicTerm, false);
    }

    private AcademicInterval getAcademicInterval(String academicTerm, Boolean nullDefault) {

        if (Strings.isNullOrEmpty(academicTerm)) {
            return nullDefault ? null : getDefaultAcademicTerm();
        }

        ExecutionInterval interval = ExecutionInterval.getExecutionInterval(academicTerm);

        if (interval == null) {
            throw newApplicationError(Status.NOT_FOUND, "resource_not_found", "Can't find the academic term : " + academicTerm);
        }

        return interval.getAcademicInterval();

    }

    private void validateDay(String day) {
        if (day != null && !Strings.isNullOrEmpty(day.trim())) {
            boolean invalid = false;
            try {
                DateTime parse = formatDay.parseDateTime(day);
                invalid = parse == null;
            } catch (IllegalArgumentException | UnsupportedOperationException e) {
                invalid = true;
            } finally {
                if (invalid) {
                    throw newApplicationError(Status.BAD_REQUEST, "format_error", "day must be " + dayPattern);
                }
            }
        }
    }
}
