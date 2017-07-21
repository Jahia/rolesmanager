<%@ page language="java" contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="functions" uri="http://www.jahia.org/tags/functions" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%--@elvariable id="flowRequestContext" type="org.springframework.webflow.execution.RequestContext"--%>
<%--@elvariable id="handler" type="org.jahia.modules.rolesmanager.RolesAndPermissionsHandler"--%>
<template:addResources type="javascript" resources="jquery.min.js,jquery-ui.min.js,jquery.blockUI.js,workInProgress.js,admin-bootstrap.js,bootbox.min.js"/>
<template:addResources type="css" resources="jquery-ui.smoothness.css,jquery-ui.smoothness-jahia.css"/>
<fmt:message key="label.workInProgressTitle" var="i18nWaiting"/><c:set var="i18nWaiting" value="${functions:escapeJavaScript(i18nWaiting)}"/>
<fmt:message var="i18nDeepCopy" key="rolesmanager.rolesAndPermissions.deepCopy"/>
<fmt:message var="i18nHelp" key="rolesmanager.rolesAndPermissions.deepCopy.help"/>
<fmt:message var="i18nDeepCopyHelp" key="rolesmanager.rolesAndPermissions.deepCopy.description"/>
<template:addResources>
    <script type="text/javascript">
        $(document).ready(function() {
            $('#newRole').focus();
            $('#newRole').select();
            $('#newRole').click(function() {
                $(this).select();
            });
            $('#copySubRolesHelp').click(function () {
                bootbox.alert({
                    title: '${functions:escapeJavaScript(i18nDeepCopy)}',
                    message: '${functions:escapeJavaScript(i18nDeepCopyHelp)}'
                });
            });
        })
    </script>
</template:addResources>
<div>
    <p>
        <c:forEach var="msg" items="${flowRequestContext.messageContext.allMessages}">
            <div class="alert ${msg.severity == 'ERROR' ? 'validationError' : ''} ${msg.severity == 'ERROR' ? 'alert-error' : 'alert-success'}">
                <button type="button" class="close" data-dismiss="alert">&times;</button>
                    ${fn:escapeXml(msg.text)}
            </div>
        </c:forEach>
    </p>

    <h2><fmt:message key="rolesmanager.rolesAndPermissions.copyRole"/>: ${handler.roleBean.name}</h2>

    <div class="box-1">
        <form action="${flowExecutionUrl}" method="post" autocomplete="off">
            <input type="hidden" name="uuid" value="${handler.roleBean.uuid}"/>
            <fieldset>
                <div class="container-fluid">
                    <div class="row-fluid">
                        <div class="span4">
                            <label for="newRole"><fmt:message key="label.name"/> <span class="text-error"><strong>*</strong></span></label>
                            <input type="text" name="newRole" class="span12" id="newRole" value="${fn:escapeXml(handler.roleBean.name)}-copy"/>
                        </div>
                    </div>

                    <c:if test="${fn:length(handler.roleBean.subRoles) > 0}">
	                    <div class="row-fluid">
	                        <div class="span4">
	                            <label for="deepCopy" class="checkbox">
	                                <input type="checkbox" name="deepCopy" id="deepCopy" checked="checked"> ${fn:escapeXml(i18nDeepCopy)}
	                                <a id="copySubRolesHelp" title="${fn:escapeXml(i18nHelp)}" href="#copySubRolesHelp"><img src="<c:url value='/icons/help.png'/>" width="16" height="16" alt="${fn:escapeXml(i18nHelp)}"></a>
	                            </label>
	                        </div>
	                    </div>
                    </c:if>
                </div>
            </fieldset>

            <fieldset>
                <div class="container-fluid">
                    <div class="row-fluid">
                        <div class="span12">
                            <button class="btn btn-primary" type="submit" name="_eventId_copy" onclick="workInProgress('${i18nWaiting}'); return true;">
                                <i class="icon-share icon-white"></i>
                                &nbsp;<fmt:message key="label.copy"/>
                            </button>
                            <button class="btn" type="submit" name="_eventId_cancel">
                                <i class="icon-ban-circle"></i>
                                &nbsp;<fmt:message key="label.cancel"/>
                            </button>
                        </div>
                    </div>
                </div>
            </fieldset>
        </form>
    </div>
</div>