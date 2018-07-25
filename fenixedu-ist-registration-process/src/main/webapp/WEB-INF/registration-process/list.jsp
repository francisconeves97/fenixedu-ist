<%--

    Copyright © 2018 Instituto Superior Técnico

    This file is part of FenixEdu Spaces.

    FenixEdu Spaces is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    FenixEdu Spaces is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with FenixEdu Spaces.  If not, see <http://www.gnu.org/licenses/>.

--%>

<!DOCTYPE html> 
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<div class="page-header">
    <h2>
        <spring:message code="title.registration.process.signed.declaration"/>
    </h2>
</div>
<c:if test="${!errors.isEmpty()}">
	<div class="error0">
	<table>
		<c:forEach var="error" items="${errors}">      
			<tr>
				<td class="error0"> <spring:message code="${error}"></spring:message> </td>
			</tr>
		</c:forEach>
	</table>
	</div>
	<br>
	<br>
</c:if>

<%-- Display Declarations Interface --%>

<p> 
	<strong> <spring:message code="label.declaration.view.file.view.title"/> </strong> 
</p>

<c:if test="${not declarationRegistrationFiles.isEmpty()}">
	<table class="tstyle1 thlight thright mtop025 table">
	    <thead>
	        <th><spring:message code="label.declaration.view.file.requestor" /></th>
	        <th><spring:message code="label.declaration.view.file.date.creation"/></th>
			<th><spring:message code="label.declaration.view.file.name"/></th>
			<th><spring:message code="label.declaration.view.file.language"/></th>
			<th><spring:message code="label.declaration.view.file.year"/></th>
			<th><spring:message code="label.declaration.view.file.state"/></th>
			<th><spring:message code="label.declaration.view.file.last.update"/></th>
			<th><spring:message code="label.declaration.view.file.header.retry.workflow"/></th>
			<th></th>
	    </thead>
	    <tbody>
	        <c:forEach var="file" items="${declarationRegistrationFiles}">               
		    	<tr>
	        		<td>
	        			<c:if test="${file.creator == null}">
	        				<spring:message code="label.declaration.view.not.defined"/>
	        			</c:if>
	        			<c:if test="${file.creator != null}">
	        				<c:out value="${file.creator.displayName}" />
	        			</c:if>	        			
	        		</td>
	        		<td>
	        			<c:out value="${file.creationDate.toString(\"dd-MM-yyyy kk:mm:ss\")}"/>
	        		</td>
	        		<td>
	        			<c:out value="${file.displayName}"/>
	        		</td>
	        		<td>
	        			<c:out value="${file.locale.displayLanguage}"/>
	        		</td>
	        		<td>
	        			<c:out value="${file.executionYear.name}"/>
	        		</td>
	        		<td>
	        			<c:if test="${file.state == null}">
	        				<spring:message code="label.declaration.view.not.defined"/>
	        			</c:if>
	        			<c:if test="${file.state != null}">
	        				<c:out value="${file.state.localizedName}"/>
	        			</c:if>
	        		</td>	        		
	        		<td>
	        			<c:out value="${file.lastUpdated.toString(\"dd-MM-yyyy kk:mm:ss\")}"/>        			
	        		</td>
	        		<td>
	        			<c:if test="${file.state != 'STORED'}">
	        				<spring:url var="retryWorkflow" value="/registration-process-signed-declaration/retry-declaration-workflow/registration/${registration.externalId}/file/${file.externalId}" />
							<a href="${retryWorkflow}"><spring:message code="label.declaration.view.file.retry.workflow"/></a>
	        			</c:if>	  
	        		</td>
	        		<td>
	        			<c:if test="${file.downloadSignedFileLink == null}">
	        				<spring:url var="downloadFile" value="/registration-process-signed-declaration/view-files/registration/${registration.externalId}/download/file/${file.externalId}" />
							<a href="${downloadFile}"><spring:message code="label.declaration.view.file.view"/></a>
	        			</c:if>
	        			<c:if test="${file.downloadSignedFileLink != null}">
	        				<spring:url var="downloadFileFromDrive" value="${file.downloadSignedFileLink}" />
							<a href="${downloadFileFromDrive}"><spring:message code="label.declaration.view.file.view"/></a>
	        			</c:if>             	            	
	        		</td>
	        	</tr>            
	        </c:forEach>
	    </tbody>    
	</table>
</c:if>

<c:if test="${declarationRegistrationFiles.isEmpty()}">
	<em> <spring:message code="label.declaration.view.file.no.files.generated"/></em>
	<br>
	<br>
</c:if>

<br>

<%-- Generate Declarations Interface --%>

<p class="mbottom025">
		<strong> <spring:message code="label.declaration.generate.file.title"/> </strong>
</p>
  
<spring:url var="requestDeclaration" value="/registration-process-signed-declaration/generate-declaration/registration/${registration.externalId}"/>
<form:form modelAttribute="declarationTemplateInputFormBean" method="POST" action="${requestDeclaration}" class="form-horizontal">
	${csrf.field()}
	
	
	<div class="tstyle5 thright thlight mtop025 mbottom0 thmiddle">
		<div class="form-group">
			<form:label path="declarationTemplate" class="control-label col-sm-2"> <spring:message code="label.declaration.generate.file.choose.template" /> </form:label>			
			<div class="col-sm-10">
				<form:select path="declarationTemplate" class="form-control">
	   				<form:options items="${declarationTemplates}" itemValue="externalId" itemLabel="displayName.content"/>
				</form:select>
			</div>
		</div>
	</div>
	
	<div class="tstyle5 thright thlight mtop025 mbottom0 thmiddle">
		<div class="form-group">
			<form:label path="executionYear" class="control-label col-sm-2"> <spring:message code="label.declaration.generate.file.choose.year" /> </form:label>
			<div class="col-sm-10">
				<form:select path="executionYear" class="form-control">
	   				<form:options items="${registration.registrationDataByExecutionYear}" itemValue="executionYear.externalId" itemLabel="executionYear.name"/>
				</form:select>
			</div>
		</div>
	</div>

	<p class="mtop15">
		<button type="submit" class="btn btn-primary"><spring:message code="label.declaration.generate.file.button"/></button>
	</p>

</form:form>
