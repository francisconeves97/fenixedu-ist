<!DOCTYPE html>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>

<div>
<div class="page-header">
  <h1>Delegados<small>Atribuir Cargo de Delegado</small></h1>
</div>

<spring:url var="formActionUrl" value="${action}"/>
<b>Selecione o aluno pretendido para o cargo ${delegatePositionBean.delegateTitle}</b><p>
<div>
<form:form modelAttribute="delegatePositionBean" role="form" method="post" action="${formActionUrl}" enctype="multipart/form-data">
	<form:input type="hidden" class="form-control" id="cycleTypeInput" path="cycleType" placeholder="" value="${delegatePositionBean.cycleType}"/>
	<c:if test="${not empty delegatePositionBean.delegate}">
    	<form:input type="hidden" class="form-control" id="delegateInput" path="delegate" placeholder="" value="${delegatePositionBean.delegate.externalId}"/>
    </c:if>
    <c:if test="${not empty delegatePositionBean.degree}">
    	<form:input type="hidden" class="form-control" id="degreeInput" path="degree" placeholder="" value="${delegatePositionBean.degree.externalId}"/>
    </c:if>
    <c:if test="${not empty delegatePositionBean.curricularYear}">
    	<form:input type="hidden" class="form-control" id="curricularYearInput" path="curricularYear" placeholder="" value="${delegatePositionBean.curricularYear.externalId}"/>   
	</c:if>
	<div class="form-group">
	    <form:label for="nameInput" path="name">Aluno</form:label>
	    <form:input type="text" class="form-control" id="nameInput" path="name" placeholder="" required="required"/>
  	</div>
  	<button type="submit" class="btn btn-default"><spring:message code="label.submit"/></button>
</form:form>
</div>
</div>