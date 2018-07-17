/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST Tutorship.
 *
 * FenixEdu IST Tutorship is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST Tutorship is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST Tutorship.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.tutorship.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.ExecutionDegree;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Teacher;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicInterval;
import org.joda.time.DateTime;

public class TutorshipIntention extends TutorshipIntention_Base {

    static final public Comparator<TutorshipIntention> COMPARATOR_FOR_ATTRIBUTING_TUTOR_STUDENTS =
            new Comparator<TutorshipIntention>() {

                @Override
                public int compare(TutorshipIntention ti1, TutorshipIntention ti2) {
                    return ti1.getTeacher().getPerson().getName().compareTo(ti2.getTeacher().getPerson().getName());
                }
            };

    public TutorshipIntention(DegreeCurricularPlan dcp, Teacher teacher, AcademicInterval interval) {
        super();
        setDegreeCurricularPlan(dcp);
        setTeacher(teacher);
        setAcademicInterval(interval);
        checkOverlaps();
    }

    private void checkOverlaps() {
        for (TutorshipIntention intention : getDegreeCurricularPlan().getTutorshipIntentionSet()) {
            if (!intention.equals(this) && intention.getTeacher().equals(getTeacher())
                    && intention.getAcademicInterval().overlaps(getAcademicInterval())) {
                throw new DomainException("error.tutorship.overlapingTutorshipIntentions");
            }
        }
    }

    public boolean isDeletable() {
        for (Tutorship tutorship : getTeacher().getTutorshipsSet()) {
            if (tutorship.getStudentCurricularPlan().getDegreeCurricularPlan().equals(getDegreeCurricularPlan())
                    && getAcademicInterval().contains(tutorship.getStartDate().toDateTime(new DateTime(0)))) {
                return false;
            }
        }
        return true;
    }

    public List<Tutorship> getTutorships() {
        List<Tutorship> result = new ArrayList<Tutorship>();
        for (Tutorship tutorship : getTeacher().getTutorshipsSet()) {
            if (tutorship.getStudentCurricularPlan().getDegreeCurricularPlan().equals(getDegreeCurricularPlan())
                    && getAcademicInterval().contains(tutorship.getStartDate().toDateTime(new DateTime(0)))) {
                result.add(tutorship);
            }
        }
        return result;
    }

    public void delete() {
        setDegreeCurricularPlan(null);
        setTeacher(null);
        deleteDomainObject();
    }

    public static TutorshipIntention readByDcpAndTeacherAndInterval(DegreeCurricularPlan dcp, Teacher teacher,
            AcademicInterval academicInterval) {
        for (TutorshipIntention intention : teacher.getTutorshipIntentionSet()) {
            if (intention.getDegreeCurricularPlan().equals(dcp) && intention.getAcademicInterval().equals(academicInterval)) {
                return intention;
            }
        }
        return null;
    }

    public static Map<String, List<TutorshipIntention>> getTutorshipIntentions(ExecutionYear executionYear) {
        Map<String, List<TutorshipIntention>> result = new LinkedHashMap<>();

        List<ExecutionDegree> degrees = executionYear.getExecutionDegreesSet().stream()
                .filter(executionDegree -> executionDegree.getDegreeType().isIntegratedMasterDegree() || executionDegree.getDegreeType().isBolonhaDegree())
                .sorted(ExecutionDegree.EXECUTION_DEGREE_COMPARATORY_BY_DEGREE_TYPE_AND_NAME)
                .collect(Collectors.toList());

        for (ExecutionDegree executionDegree : degrees) {

            List<TutorshipIntention> degreeTutorshipIntentions = new ArrayList<>();
            for (TutorshipIntention tutorshipIntention : executionDegree.getDegreeCurricularPlan().getTutorshipIntentionSet()) {
                if (tutorshipIntention.getAcademicInterval().equals(executionDegree.getAcademicInterval())) {
                    degreeTutorshipIntentions.add(tutorshipIntention);
                }
            }
            degreeTutorshipIntentions.sort(TutorshipIntention.COMPARATOR_FOR_ATTRIBUTING_TUTOR_STUDENTS);
            result.put(executionDegree.getPresentationName(), degreeTutorshipIntentions);
        }


        return result;
    }

    public static List<TutorshipIntention> getTutorshipIntentions(ExecutionDegree executionDegree) {
        List<TutorshipIntention> result = new ArrayList<TutorshipIntention>();
        for (TutorshipIntention tutorshipIntention : executionDegree.getDegreeCurricularPlan().getTutorshipIntentionSet()) {
            if (tutorshipIntention.getAcademicInterval().equals(executionDegree.getAcademicInterval())) {
                result.add(tutorshipIntention);
            }
        }
        Collections.sort(result, TutorshipIntention.COMPARATOR_FOR_ATTRIBUTING_TUTOR_STUDENTS);
        return result;
    }

}
