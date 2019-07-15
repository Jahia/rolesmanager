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
<template:addResources type="javascript" resources="jquery.min.js,jquery-ui.min.js,jquery.blockUI.js,workInProgress.js,bootbox.min.js"/>
<template:addResources type="css" resources="jquery-ui.smoothness.css,jquery-ui.smoothness-jahia.css,rolesmanager.css"/>
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
<div class="page-header">
    <h2><fmt:message key="rolesmanager.rolesAndPermissions.copyRole"/>: ${handler.roleBean.name}</h2>
</div>

<c:forEach var="msg" items="${flowRequestContext.messageContext.allMessages}">
    <div class="alert ${msg.severity == 'ERROR' ? 'validationError' : ''} ${msg.severity == 'ERROR' ? ' alert-danger' : ' alert-success'}">
        <button type="button" class="close" data-dismiss="alert">&times;</button>
            ${fn:escapeXml(msg.text)}
    </div>
</c:forEach>

<div class="col-sm-6 col-sm-offset-3">
    <div class="panel panel-default">
        <div class="panel-body">
            <form action="${flowExecutionUrl}" method="post" autocomplete="off">
                <input type="hidden" name="uuid" value="${handler.roleBean.uuid}"/>

                <div class="row">
                    <div class="col-md-12">
                        <div class="form-group label-floating ">
                            <label class="control-label" for="newRole"><fmt:message key="label.name"/>
                                <span class="text-error"><strong>*</strong></span></label>
                            <input type="text" name="newRole" class="form-control" id="newRole"
                                   value="${fn:escapeXml(handler.roleBean.name)}-copy"/>
                        </div>
                    </div>
                </div>


                <c:if test="${fn:length(handler.roleBean.subRoles) > 0}">
                    <div class="row">
                        <div class="col-md-12">
                            <div class="form-group">
                                <div class="input-group">
                                    <div class="checkbox">
                                        <label for="deepCopy" class="checkbox">
                                            <input id="deepCopy" name="deepCopy" type="checkbox" checked="checked">
                                            ${fn:escapeXml(i18nDeepCopy)}
                                        </label>
                                    </div>
                                    <span class="input-group-btn">
                                        <a id="copySubRolesHelp" class="btn btn-primary btn-fab btn-fab-xs"
                                           data-toggle="tooltip" data-container="body"
                                           title="${fn:escapeXml(i18nHelp)}" href="#copySubRolesHelp">
                                            <i class="material-icons" alt="${fn:escapeXml(i18nHelp)}">info</i>
                                        </a>
                                    </span>
                                </div>
                            </div>
                        </div>
                    </div>
                </c:if>

                <div class="form-group from-group-sm">
                    <button class="btn btn-primary btn-raised pull-right" type="submit" name="_eventId_copy"
                            onclick="workInProgress('${i18nWaiting}'); return true;">
                        <fmt:message key="label.copy"/>
                    </button>
                    <button class="btn btn-default pull-right" type="submit" name="_eventId_cancel">
                        <fmt:message key="label.cancel"/>
                    </button>
                </div>
            </form>
        </div>
    </div>
</div>
