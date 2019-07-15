<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="functions" uri="http://www.jahia.org/tags/functions" %>

<%--@elvariable id="handler" type="org.jahia.modules.rolesmanager.RolesAndPermissionsHandler"--%>
<%--@elvariable id="flowRequestContext" type="org.springframework.webflow.execution.RequestContext"--%>

<jcr:node var="sites" path="/sites"/>
<jcr:nodeProperty name="j:defaultSite" node="${sites}" var="defaultSite"/>
<c:set var="defaultPrepackagedSite" value="acmespace.zip"/>
<template:addResources type="javascript"
                       resources="jquery.min.js,jquery-ui.min.js,workInProgress.js"/>

<template:addResources type="css" resources="jquery-ui.smoothness.css,jquery-ui.smoothness-jahia.css,tristate.css,rolesmanager.css"/>

<jsp:useBean id="nowDate" class="java.util.Date"/>
<fmt:formatDate value="${nowDate}" pattern="yyyy-MM-dd-HH-mm" var="now"/>
<fmt:message key="label.workInProgressTitle" var="i18nWaiting"/><c:set var="i18nWaiting"
                                                                       value="${functions:escapeJavaScript(i18nWaiting)}"/>
<template:addResources>
    <script type="text/javascript">
        $(document).ready(function () {
            $("#form").submit(function () {
                selectedPermissions = [];
                $(".checkbox[class*='checked']").each(function (i) {
                    selectedPermissions[i] = $(this).attr('path');
                });
                partialSelectedPermissions = [];
                $(".checkbox[class*='partial']").each(function (i) {
                    partialSelectedPermissions[i] = $(this).attr('path');
                });

                $("#selectedPermissions").val(selectedPermissions);
                $("#partialSelectedPermissions").val(partialSelectedPermissions);
            });

            $(".checkbox").click(function () {
                $('.submitButton').addClass('btn-danger');
                if ($(this).hasClass('checked')) {
                    uncheck(this);
                } else if ($(this).hasClass('partial')) {
                    check(this);
                } else {
                    check(this);
                }

                return false;
            });

            function uncheckthis() {
                uncheck(this);
            }

            function uncheck(obj) {
                $(obj).removeClass('checked');
                $(obj).removeClass('partial');
                $(":checkbox[name='selectedPermissions'][path='" + $(obj).attr('path') + "']").attr('checked', false);
                $(":checkbox[name='partialSelectedPermissions'][path='" + $(obj).attr('path') + "']").attr('checked',
                    false);

                // Was checked, uncheck all
                selector = ".checkbox[parent='" + $(obj).attr('path') + "']";

                $(selector).each(uncheckthis);

                setParent($(obj).attr('parent'));
            }

            function checkthis() {
                check(this);
            }

            function check(obj) {
                $(obj).addClass('checked');
                $(obj).removeClass('partial');

                $(":checkbox[name='selectedPermissions'][path='" + $(obj).attr('path') + "']").attr('checked', true);
                $(":checkbox[name='partialSelectedPermissions'][path='" + $(obj).attr('path') + "']").attr('checked',
                    false);

                // Was unchecked, check all
                selector = ".checkbox[parent='" + $(obj).attr('path') + "']";

                $(selector).each(checkthis);

                setParent($(obj).attr('parent'));
            }

            function setParent(parentPath) {
                selector = ".checkbox[path='" + parentPath + "']";

                if ($(".checkbox[parent='" + parentPath + "'][class*='checked']").length ==
                    $(".checkbox[parent='" + parentPath + "']").length) {
                    $(selector).addClass('checked');
                    $(selector).removeClass('partial');
                    $(":checkbox[name='selectedPermissions'][path='" + parentPath + "']").attr('checked', true);
                    $(":checkbox[name='partialSelectedPermissions'][path='" + parentPath + "']").attr('checked', false);
                } else if ($(".checkbox[parent='" + parentPath + "'][class*='checked']").length > 0 ||
                    $(".checkbox[parent='" + parentPath + "'][class*='partial']").length > 0) {
                    $(selector).removeClass('checked');
                    $(selector).addClass('partial');
                    $(":checkbox[name='selectedPermissions'][path='" + parentPath + "']").attr('checked', false);
                    $(":checkbox[name='partialSelectedPermissions'][path='" + parentPath + "']").attr('checked', true);
                } else {
                    $(selector).removeClass('checked');
                    $(selector).removeClass('partial');
                    $(":checkbox[name='selectedPermissions'][path='" + parentPath + "']").attr('checked', false);
                    $(":checkbox[name='partialSelectedPermissions'][path='" + parentPath + "']").attr('checked', false);
                }

                p = $(".checkbox[path='" + parentPath + "']").attr("parent");
                if (p) {
                    setParent(p);
                }
            }

            $('#addContextField').keypress(function (e) {
                // Enter pressed?
                if (e.which == 10 || e.which == 13) {
                    $("#addContextButton").click();
                    return false;
                }

            });
        });

        function addI18nRow() {
            var newLanguage = $("#newLanguage");
            var languageCode = newLanguage.val();
            if (languageCode != null) {
                var $selectedOption = newLanguage.find("option[value='" + languageCode + "']");
                var languageName = $selectedOption.text();
                $('#roleI18nPropsTable').find('> tbody:last').append("<tr><td>" + languageName +
                    "<input type=\"hidden\" name=\"languageCode\" value=\"" +
                    languageCode + "\" />" +
                    "<input type=\"hidden\" name=\"languageName\" value=\"" +
                    languageName + "\" />" +
                    "</td><td><input type=\"text\" name=\"title\" class=\"form-control\" onchange=\"$('.submitButton').addClass('btn-danger')\" /></td>" +
                    "<td><textarea name=\"description\" class=\"form-control\" onchange=\"$('.submitButton').addClass('btn-danger')\" /></td></tr>");
                $selectedOption.remove();
            }
        }

        function updatedSelectedTypes() {
            $('#selectedAvailableNodeTypes').empty();
            $('#nodeTypes').find('option:selected').each(function () {
                $('#selectedAvailableNodeTypes').append("<li>" + $(this).text() + "</li>");
            });
        }

        function viewRole(uuid) {
            var form = $('#roleForm');
            form.find(".roleUuid").val(uuid);
            form.find(".eventId").val("viewRole");
            form.submit();
        }
    </script>
</template:addResources>

<div class="page-header">
    <h2><fmt:message key="rolesmanager.rolesAndPermissions.roleDetails"/> : ${handler.roleBean.name}</h2>
</div>

<c:forEach var="msg" items="${flowRequestContext.messageContext.allMessages}">
    <div class="alert ${msg.severity == 'ERROR' ? 'validationError' : ''} ${msg.severity == 'ERROR' ? ' alert-danger' : ' alert-success'}">
        <button type="button" class="close" data-dismiss="alert">&times;</button>
            ${fn:escapeXml(msg.text)}
    </div>
</c:forEach>

<div class="panel panel-default">
    <div class="panel-body">
        <form id="roleForm" style="display: none" action="${flowExecutionUrl}" method="POST" onsubmit="workInProgress('${i18nWaiting}');">
            <input type="hidden" name="_eventId" class="eventId"/>
            <input type="hidden" name="uuid" class="roleUuid"/>
        </form>

        <div>

            <form id="form" action="${flowExecutionUrl}" method="post">
                <div class="row">
                    <div class="col-md-6">
                        <button class="btn btn-default btn-raised" type="submit" name="_eventId_rolesList">
                            <fmt:message key="rolesmanager.rolesAndPermissions.backToRolesList"/>
                        </button>

                        <c:if test="${not empty handler.roleBean.parentUuid}">
                            <button class="btn btn-default btn-raised" onclick="viewRole('${handler.roleBean.parentUuid}'); return false;">
                                <fmt:message key="rolesmanager.rolesAndPermissions.goToParentRole"/>
                            </button>
                        </c:if>
                    </div>
                    <div class="col-md-6">
                        <button class="submitButton btn ${handler.roleBean.dirty ? 'btn-danger' : 'btn-primary'} btn-raised pull-right" type="submit"
                                name="_eventId_saveRole">
                            <fmt:message key="label.save"/>
                        </button>
                        <button class="submitButton btn ${handler.roleBean.dirty ? 'btn-danger' : 'btn-default'} pull-right" type="submit"
                                name="_eventId_revertRole">
                            <fmt:message key="label.cancel"/>
                        </button>
                    </div>
                </div>

                <div class="row">
                    <div class="col-md-12">
                        <div class="tabbable">
                            <ul class="nav nav-tabs">
                                <li class="active">
                                    <a href="#tabPermissions" data-toggle="tab">
                                        <fmt:message key="rolesmanager.rolesAndPermissions.permissions"/>
                                    </a>
                                </li>
                                <li>
                                    <a href="#tabLabel" data-toggle="tab">
                                        <fmt:message key="rolesmanager.rolesAndPermissions.labels"/>
                                    </a>
                                </li>
                                <li>
                                    <a href="#tabTypes" data-toggle="tab">
                                        <fmt:message key="rolesmanager.rolesAndPermissions.nodeTypes"/>
                                    </a>
                                </li>
                                <li>
                                    <a href="#tabSub" data-toggle="tab">
                                        <fmt:message key="rolesmanager.rolesAndPermissions.subRoles"/>
                                    </a>
                                </li>
                            </ul>
                            <div class="tab-content">
                                <div id="tabPermissions" class="tab-pane active">
                                    <div>
                                        <c:forEach items="${handler.roleBean.permissions}" var="centry" varStatus="status">
                                            <p>
                                                <c:set var="key">${centry.key}.${handler.roleBean.roleType.name}</c:set>
                                                <c:set var="key" value="${fn:replace(key,'-','_')}"/>
                                                <c:set var="key" value="${fn:replace(key,'/','_')}"/>
                                                <fmt:message key="rolesmanager.rolesAndPermissions.context.${key}" var="label"/>
                                                <c:if test="${not fn:startsWith(label, '???')}">
                                                    ${label}:
                                                </c:if>
                                                <c:if test="${fn:startsWith(label, '???')}">
                                                    <fmt:message key="rolesmanager.rolesAndPermissions.context"/>&nbsp;${permissionGroup.key}:
                                                </c:if>

                                                <c:forEach items="${centry.value}" var="gentry" varStatus="status">
                                                    <c:forEach items="${gentry.value}" var="entry">
                                                        <c:set value="${entry.value}" var="permission"/>
                                                        <c:if test="${(permission.set or permission.superSet) and not (centry.value[gentry.key][permission.parentPath].set or centry.value[gentry.key][permission.parentPath].superSet)}">
                                                            <c:choose>
                                                                <c:when test="${centry.key ne handler.currentContext}">
                                                                    <a href="#"
                                                                       onclick="$('#form').attr('action',$('#form').attr('action') + '#${permission.path}');$('#contextSelector').val('${centry.key}');$('#tabField').val('${gentry.key}');$('#eventField').attr('name','_eventId_switchGroup');$('#form').submit()"> ${permission.title} </a>
                                                                    |
                                                                </c:when>
                                                                <c:when test="${gentry.key ne handler.currentGroup}">
                                                                    <a href="#"
                                                                       onclick="$('#form').attr('action',$('#form').attr('action') + '#${permission.path}');$('#switchToGroup${status.index}').click()"> ${permission.title} </a>
                                                                    |
                                                                </c:when>
                                                                <c:otherwise>
                                                                    <a href="#${permission.path}"> ${permission.title} </a>
                                                                    |
                                                                </c:otherwise>
                                                            </c:choose>
                                                        </c:if>
                                                    </c:forEach>
                                                </c:forEach>
                                            </p>
                                        </c:forEach>
                                    </div>

                                    <div id="permissionsTree">
                                        <input type="hidden" id="eventField" name="eventField" value="on"/>
                                        <input type="hidden" id="permissionField" name="permission"/>
                                        <select id="contextSelector" name="context" class="form-control"
                                                onchange="$('#eventField').attr('name','_eventId_switchContext');$('#form').submit()">
                                            <c:forEach items="${handler.roleBean.permissions}" var="permissionGroup">
                                                <option ${flowRequestContext.currentState.id eq 'viewRole' and handler.currentContext eq permissionGroup.key ? 'selected':''}
                                                        value="${permissionGroup.key}">
                                                    <c:set var="key">${permissionGroup.key}.${handler.roleBean.roleType.name}</c:set>
                                                    <c:set var="key" value="${fn:replace(key,'-','_')}"/>
                                                    <c:set var="key" value="${fn:replace(key,'/','_')}"/>
                                                    <fmt:message key="rolesmanager.rolesAndPermissions.context.${key}" var="label"/>
                                                    <c:if test="${not fn:startsWith(label, '???')}">
                                                        ${label}
                                                    </c:if>
                                                    <c:if test="${fn:startsWith(label, '???')}">
                                                        <fmt:message key="rolesmanager.rolesAndPermissions.context"/> &nbsp; ${permissionGroup.key}
                                                    </c:if>
                                                </option>
                                            </c:forEach>
                                        </select>

                                        <input type="hidden" name="groupTab" id="tabField" value=""/>

                                        <div class="btn-group">
                                            <div class="btn-group">
                                                <c:forEach items="${handler.roleBean.permissions[handler.currentContext]}" var="permissionGroup" varStatus="status">
                                                    <button class="btn ${handler.currentGroup eq permissionGroup.key ? 'btn-primary btn-raised':''}"
                                                            id="switchToGroup${status.index}" type="submit" name="_eventId_switchGroup"
                                                            onclick="$('#tabField').val('${permissionGroup.key}')">
                                                        <c:set var="key" value="${fn:replace(permissionGroup.key,',','_')}"/>
                                                        <c:set var="key" value="${fn:replace(key,'-','_')}"/>
                                                        <fmt:message key="rolesmanager.rolesAndPermissions.group.${key}"/>
                                                    </button>
                                                </c:forEach>
                                            </div>
                                        </div>


                                        <fieldset>
                                            <c:if test="${not empty handler.roleBean.permissions[handler.currentContext][handler.currentGroup]}">
                                                <table class="table table-striped">
                                                    <thead>
                                                    <tr>
                                                        <th width="3%">&nbsp;</th>
                                                        <th width="40%">
                                                            <fmt:message key="label.name"/>
                                                        </th>
                                                        <th width="57%">
                                                            <fmt:message key="label.description"/>
                                                        </th>
                                                    </tr>
                                                    </thead>

                                                    <tbody>

                                                    <c:forEach items="${handler.roleBean.permissions[handler.currentContext][handler.currentGroup]}" var="entry">
                                                        <c:set value="${entry.value}" var="permission"/>
                                                        <tr>
                                                            <td>
                                                                <a name="${permission.path}"></a>

                                                                <div class="triState" style="height:13px; overflow: hidden">

                                                                    <c:choose>
                                                                        <c:when test="${permission.superSet}"><span class="checkbox super-checked"></span></c:when>
                                                                        <c:when test="${permission.set}">
                                                                            <div class="checkbox">
                                                                                <label>
                                                                                    <a class="checkbox checked" path="${permission.path}"
                                                                                       parent="${permission.parentPath}" href=""></a>
                                                                                    <span class="checkbox checkbox-material"></span>
                                                                                </label>
                                                                            </div>
                                                                        </c:when>
                                                                        <c:when test="${permission.partialSet}">
                                                                            <a class="checkbox partial" path="${permission.path}" parent="${permission.parentPath}"
                                                                               href=""></a>
                                                                        </c:when>
                                                                        <c:otherwise>
                                                                            <a class="checkbox" path="${permission.path}" parent="${permission.parentPath}"
                                                                               href=""></a>
                                                                        </c:otherwise>
                                                                    </c:choose>

                                                                </div>
                                                            </td>
                                                            <td>
                                                                <c:forEach var="i" begin="3" end="${permission.depth}" step="1" varStatus="status5">
                                                                    &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
                                                                </c:forEach>
                                                                    ${permission.depth == 2 ? '<h3>' : '' }${permission.title} ${permission.depth == 2 ? '</h3>' : '' }

                                                                <c:if test="${not empty permission.mappedPermissions and not permission.partialSet and not permission.hasChildren}">
                                                                    <a href="#"
                                                                       onclick="$('#permissionField').val('${permission.path}');$('#eventField').attr('name','_eventId_expandMappedPermissions');$('#form').submit()">
                                                                        <c:if test="${permission.mappedPermissionsExpanded}">-</c:if>
                                                                        <c:if test="${not permission.mappedPermissionsExpanded}">+</c:if>
                                                                    </a>
                                                                </c:if>

                                                            </td>
                                                            <td>
                                                                    ${permission.description}
                                                            </td>
                                                        </tr>
                                                    </c:forEach>
                                                    </tbody>
                                                </table>
                                            </c:if>
                                        </fieldset>
                                    </div>
                                </div>

                                <div id="tabLabel" class="tab-pane">
                                    <fieldset>
                                        <p><fmt:message key="rolesmanager.rolesAndPermissions.labels.description"/></p>

                                        <div class="row">
                                            <div class="col-sm-6 col-md-6">
                                                <div class="form-inline">
                                                    <div class="form-group">
                                                        <select id="newLanguage" class="form-control"
                                                                style="min-width: 150px">
                                                            <c:forEach items="${handler.availableLanguages}" var="entry">
                                                                <option value="${entry.key}">${entry.value}</option>
                                                            </c:forEach>
                                                        </select>
                                                        <button class="btn btn-primary btn-fab btn-fab-mini"
                                                                onclick="addI18nRow(); return false;">
                                                            <i class="material-icons">add</i>
                                                        </button>
                                                    </div>
                                                </div>
                                            </div>
                                        </div>

                                        <table class="table table-striped" id="roleI18nPropsTable">
                                            <thead>
                                            <tr>
                                                <th width="10%"><fmt:message key="label.language"/></th>
                                                <th width="20%"><fmt:message key="label.title"/></th>
                                                <th width="70%"><fmt:message key="label.description"/></th>
                                            </tr>
                                            </thead>

                                            <tbody>
                                            <c:forEach items="${handler.roleBean.i18nProperties}" var="entry" varStatus="loopStatus">
                                                <c:if test="${not empty entry.value}">
                                                    <tr>
                                                        <td>
                                                                ${entry.value.language}
                                                            <input type="hidden" name="languageCode" value="${entry.key}"/>
                                                            <input type="hidden" name="languageName" value="${entry.value.language}"/>
                                                        </td>
                                                        <td>
                                                            <div class="form-group form-adjust">
                                                                <input type="text" class="form-control" name="title" value="${entry.value.title}"
                                                                       onchange="$('.submitButton').addClass('btn-danger')"/>
                                                            </div>
                                                        </td>
                                                        <td>
                                                            <div class="form-group form-adjust">
                                                        <textarea class="form-control" name="description"
                                                                  onchange="$('.submitButton').addClass('btn-danger')">${entry.value.description}</textarea>
                                                            </div>
                                                        </td>
                                                    </tr>
                                                </c:if>
                                            </c:forEach>
                                            </tbody>
                                        </table>
                                    </fieldset>
                                </div>

                                <div id="tabTypes" class="tab-pane">
                                    <p><fmt:message key="rolesmanager.rolesAndPermissions.nodeTypes.description"/></p>

                                    <div>
                                        <div class="container">
                                            <div class="row">
                                                <div class="col-sm-1 col-md-1">
                                                    <label for="hidden"><fmt:message key="rolesmanager.rolesAndPermissions.hidden"/>
                                                        <input name="hidden" id="hidden" type="checkbox" ${handler.roleBean.hidden?'checked="true"':''}
                                                               onchange="$('.submitButton').addClass('btn-danger')"/>
                                                    </label>
                                                </div>
                                            </div>
                                            <div class="row">
                                                <c:if test="${not empty handler.roleBean.roleType.availableNodeTypes}">
                                                    <div class="col-sm-6 col-md-6">
                                                        <label for="nodeTypes">
                                                            <fmt:message key="rolesmanager.rolesAndPermissions.nodeTypes.onlyFor"/></label>
                                                        <div class="form-group form-adjust">
                                                            <select multiple="multiple" size="20" id="nodeTypes" name="nodeTypes" class="form-control"
                                                                    onchange="$('.submitButton').addClass('btn-danger'); updatedSelectedTypes();">
                                                                <c:forEach items="${handler.roleBean.nodeTypes}" var="nodeType">
                                                                    <option value="${nodeType.name}" ${nodeType.set ? 'selected="true"' : ''}>${nodeType.displayName}</option>
                                                                </c:forEach>
                                                            </select>
                                                        </div>
                                                    </div>
                                                    <div class="col-sm-6 col-md-6">
                                                        <p><fmt:message key="label.selected"/></p>
                                                        <ul id="selectedAvailableNodeTypes">
                                                            <c:forEach items="${handler.roleBean.nodeTypes}" var="nodeType">
                                                                <c:if test="${nodeType.set}">
                                                                    <li>${nodeType.displayName}</li>
                                                                </c:if>
                                                            </c:forEach>
                                                        </ul>
                                                    </div>
                                                </c:if>
                                            </div>
                                        </div>
                                    </div>

                                    <input id="selectedPermissions" type="hidden" name="selectedPermissions"/>
                                    <input id="partialSelectedPermissions" type="hidden" name="partialSelectedPermissions"/>
                                </div>

                                <div id="tabSub" class="tab-pane">
                                    <p><fmt:message key="rolesmanager.rolesAndPermissions.subRoles.description"/></p>

                                    <div class="row">
                                        <div class="col-sm-6">
                                            <div class="input-group">
                                                <input type="hidden" name="roleType" value="${handler.roleBean.roleType.name}"/>
                                                <div class="form-group label-floating">
                                                    <label class="control-label" for="function"><fmt:message
                                                            key="rolesmanager.rolesAndPermissions.subRole.name"/></label>
                                                    <input type="text" name="newRole" class="form-control"/>
                                                </div>
                                                <input type="hidden" id="uuid" name="uuid" value="${handler.roleBean.uuid}"/>
                                                <span class="input-group-btn">
                                                    <button class="btn btn-primary btn-fab btn-fab-mini"
                                                            data-toggle="tooltip" data-container="body"
                                                            data-title="<fmt:message key='rolesmanager.rolesAndPermissions.subRole.add'/>"
                                                            type="submit" name="_eventId_addRole">
                                                        <i class="material-icons">add</i>
                                                    </button>
                                                </span>
                                            </div>
                                        </div>
                                        <div class="col-sm-6">&nbsp;</div>
                                    </div>

                                    <div class="row">
                                        <div class="col-md-12">
                                            <h4><fmt:message key="rolesmanager.rolesAndPermissions.subRoles"/></h4>
                                            <c:choose>
                                                <c:when test="${not empty handler.roleBean.subRoles}">
                                                    <table class="table table-striped">
                                                        <thead>
                                                        <tr>
                                                            <th width="25%">
                                                                <fmt:message key="label.name"/>
                                                            </th>
                                                            <th width="75%">
                                                                <fmt:message key="label.description"/>
                                                            </th>
                                                        </tr>
                                                        </thead>

                                                        <tbody>
                                                        <c:forEach items="${handler.roleBean.subRoles}" var="subRole">
                                                            <tr>
                                                                <td>
                                                                    <strong><a href="#" name="_eventId_viewRole"
                                                                               onclick="viewRole('${subRole.uuid}')">${subRole.title}
                                                                        (${subRole.name})</a></strong>
                                                                </td>
                                                                <td>${subRole.description}</td>
                                                            </tr>
                                                        </c:forEach>
                                                        </tbody>
                                                    </table>
                                                </c:when>
                                                <c:otherwise><fmt:message key="rolesmanager.rolesAndPermissions.subRoles.empty"/></c:otherwise>
                                            </c:choose>
                                        </div>
                                    </div>
                                </div>
                            </div>
                            <br>
                        </div>
                    </div>
                </div>
            </form>
        </div>
    </div>
</div>
