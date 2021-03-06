/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST QUC.
 *
 * FenixEdu IST QUC is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST QUC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST QUC.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.quc.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.academic.domain.ExecutionDegree;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.Professorship;
import org.fenixedu.academic.domain.ShiftType;
import org.joda.time.DateTime;

import pt.ist.fenixedu.quc.domain.CurricularCourseInquiryTemplate;
import pt.ist.fenixedu.quc.domain.InquiryBlock;
import pt.ist.fenixedu.quc.domain.InquiryRegentAnswer;
import pt.ist.fenixedu.quc.domain.InquiryResult;
import pt.ist.fenixedu.quc.domain.InquiryResultComment;
import pt.ist.fenixedu.quc.domain.QuestionAnswer;
import pt.ist.fenixedu.quc.domain.RegentInquiryTemplate;
import pt.ist.fenixedu.quc.domain.ResultPersonCategory;
import pt.ist.fenixframework.Atomic;

import com.google.common.base.Strings;

public class RegentInquiryBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private Map<Professorship, List<TeacherShiftTypeResultsBean>> teachersResultsMap;
    private Map<ExecutionDegree, List<BlockResultsSummaryBean>> curricularBlockResultsMap;
    private Set<InquiryBlockDTO> regentInquiryBlocks;
    private Professorship professorship;

    public RegentInquiryBean(RegentInquiryTemplate regentInquiryTemplate, Professorship professorship) {
        setProfessorship(professorship);
        initCurricularBlocksResults(professorship.getExecutionCourse(), professorship.getPerson());
        initTeachersResults(professorship, professorship.getPerson());
        initRegentInquiry(regentInquiryTemplate, professorship);
    }

    private void initRegentInquiry(RegentInquiryTemplate regentInquiryTemplate, Professorship professorship) {
        setRegentInquiryBlocks(new TreeSet<InquiryBlockDTO>());
        for (InquiryBlock inquiryBlock : regentInquiryTemplate.getInquiryBlocksSet()) {
            getRegentInquiryBlocks().add(new InquiryBlockDTO(getInquiryRegentAnswer(), inquiryBlock));
        }

    }

    private void initCurricularBlocksResults(ExecutionCourse executionCourse, Person person) {
        setCurricularBlockResultsMap(new HashMap<ExecutionDegree, List<BlockResultsSummaryBean>>());
        CurricularCourseInquiryTemplate courseInquiryTemplate =
                CurricularCourseInquiryTemplate.getTemplateByExecutionPeriod(executionCourse.getExecutionPeriod());
        for (ExecutionDegree executionDegree : executionCourse.getExecutionDegrees()) {
            ArrayList<BlockResultsSummaryBean> blockResults = new ArrayList<BlockResultsSummaryBean>();
            List<InquiryResult> results =
                    InquiryResult.getInquiryResultsByExecutionDegreeAndForTeachers(executionCourse, executionDegree);
            if (results != null && results.size() > 5) {
                for (InquiryBlock inquiryBlock : courseInquiryTemplate.getInquiryBlocksSet()) {
                    blockResults.add(new BlockResultsSummaryBean(inquiryBlock, results, person, ResultPersonCategory.REGENT));
                }
                Collections.sort(blockResults, Comparator.comparing(BlockResultsSummaryBean::getInquiryBlock));
                getCurricularBlockResultsMap().put(executionDegree, blockResults);
            }
        }
    }

    private void initTeachersResults(Professorship professorship, Person person) {
        setTeachersResultsMap(new HashMap<Professorship, List<TeacherShiftTypeResultsBean>>());
        for (Professorship teacherProfessorship : professorship.getExecutionCourse().getProfessorshipsSet()) {
            ArrayList<TeacherShiftTypeResultsBean> teachersResults = new ArrayList<TeacherShiftTypeResultsBean>();
            Collection<InquiryResult> professorshipResults = teacherProfessorship.getInquiryResultsSet();
            if (!professorshipResults.isEmpty()) {
                for (ShiftType shiftType : getShiftTypes(professorshipResults)) {
                    List<InquiryResult> teacherShiftResults = InquiryResult.getInquiryResults(teacherProfessorship, shiftType);
                    if (!teacherShiftResults.isEmpty()) {
                        teachersResults.add(new TeacherShiftTypeResultsBean(teacherProfessorship, shiftType, teacherProfessorship
                                .getExecutionCourse().getExecutionPeriod(), teacherShiftResults, person,
                                ResultPersonCategory.REGENT));
                    }
                }
                Collections.sort(
                        teachersResults,
                        Comparator.comparing(TeacherShiftTypeResultsBean::getProfessorship,
                                Comparator.comparing(Professorship::getPerson, Comparator.comparing(Person::getName)))
                                .thenComparing(TeacherShiftTypeResultsBean::getShiftType));
                getTeachersResultsMap().put(teacherProfessorship, teachersResults);
            }
        }
    }

    private Set<ShiftType> getShiftTypes(Collection<InquiryResult> professorshipResults) {
        Set<ShiftType> shiftTypes = new HashSet<ShiftType>();
        for (InquiryResult inquiryResult : professorshipResults) {
            shiftTypes.add(inquiryResult.getShiftType());
        }
        return shiftTypes;
    }

    public String validateInquiry() {
        String validationResult = null;
        for (InquiryBlockDTO inquiryBlockDTO : getRegentInquiryBlocks()) {
            validationResult = inquiryBlockDTO.validateMandatoryConditions(getRegentInquiryBlocks());
            if (!Boolean.valueOf(validationResult)) {
                return validationResult;
            }
        }
        return Boolean.toString(true);
    }

    @Atomic
    public void saveChanges(Person person, ResultPersonCategory regent) {
        for (ExecutionDegree executionDegree : getCurricularBlockResultsMap().keySet()) {
            saveComments(person, regent, getCurricularBlockResultsMap().get(executionDegree));
        }
        for (Professorship professorship : getTeachersResultsMap().keySet()) {
            for (TeacherShiftTypeResultsBean teacherShiftTypeResultsBean : getTeachersResultsMap().get(professorship)) {
                saveComments(person, regent, teacherShiftTypeResultsBean.getBlockResults());
            }
        }
        for (InquiryBlockDTO blockDTO : getRegentInquiryBlocks()) {
            for (InquiryGroupQuestionBean groupQuestionBean : blockDTO.getInquiryGroups()) {
                for (InquiryQuestionDTO questionDTO : groupQuestionBean.getInquiryQuestions()) {
                    if (!Strings.isNullOrEmpty(questionDTO.getResponseValue()) || questionDTO.getQuestionAnswer() != null) {
                        if (questionDTO.getQuestionAnswer() != null) {
                            questionDTO.getQuestionAnswer().setAnswer(questionDTO.getResponseValue());
                            questionDTO.getQuestionAnswer().getInquiryAnswer().setResponseDateTime(new DateTime());
                        } else {
                            if (getInquiryRegentAnswer() == null) {
                                new InquiryRegentAnswer(getProfessorship());
                            }
                            new QuestionAnswer(getInquiryRegentAnswer(), questionDTO.getInquiryQuestion(),
                                    questionDTO.getFinalValue());
                            getInquiryRegentAnswer().setResponseDateTime(new DateTime());
                        }
                    }
                }
            }
        }
    }

    private void saveComments(Person person, ResultPersonCategory teacher, List<BlockResultsSummaryBean> blocksResults) {
        for (BlockResultsSummaryBean blockResultsSummaryBean : blocksResults) {
            for (GroupResultsSummaryBean groupResultsSummaryBean : blockResultsSummaryBean.getGroupsResults()) {
                for (QuestionResultsSummaryBean questionResultsSummaryBean : groupResultsSummaryBean.getQuestionsResults()) {
                    InquiryResult questionResult = questionResultsSummaryBean.getQuestionResult();
                    if (questionResult != null) {
                        InquiryResultComment inquiryResultComment =
                                questionResultsSummaryBean.getQuestionResult().getInquiryResultComment(person, teacher);
                        if (!Strings.isNullOrEmpty(questionResultsSummaryBean.getEditableComment())
                                || inquiryResultComment != null) {
                            if (inquiryResultComment == null) {
                                inquiryResultComment =
                                        new InquiryResultComment(questionResult, person, teacher, questionResultsSummaryBean
                                                .getQuestionResult().getInquiryResultCommentsSet().size() + 1);
                            }
                            inquiryResultComment.setComment(questionResultsSummaryBean.getEditableComment());
                        }
                    }
                }
            }
        }
    }

    public void setProfessorship(Professorship professorship) {
        this.professorship = professorship;
    }

    public Professorship getProfessorship() {
        return professorship;
    }

    public void setRegentInquiryBlocks(Set<InquiryBlockDTO> regentInquiryBlocks) {
        this.regentInquiryBlocks = regentInquiryBlocks;
    }

    public Set<InquiryBlockDTO> getRegentInquiryBlocks() {
        return regentInquiryBlocks;
    }

    public InquiryRegentAnswer getInquiryRegentAnswer() {
        return getProfessorship().getInquiryRegentAnswer();
    }

    public void setCurricularBlockResultsMap(Map<ExecutionDegree, List<BlockResultsSummaryBean>> curricularBlockResultsMap) {
        this.curricularBlockResultsMap = curricularBlockResultsMap;
    }

    public Map<ExecutionDegree, List<BlockResultsSummaryBean>> getCurricularBlockResultsMap() {
        return curricularBlockResultsMap;
    }

    public void setTeachersResultsMap(Map<Professorship, List<TeacherShiftTypeResultsBean>> teachersResultsMap) {
        this.teachersResultsMap = teachersResultsMap;
    }

    public Map<Professorship, List<TeacherShiftTypeResultsBean>> getTeachersResultsMap() {
        return teachersResultsMap;
    }
}
