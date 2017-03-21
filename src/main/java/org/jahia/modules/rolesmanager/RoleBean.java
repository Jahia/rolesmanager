/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2017 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ===================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 */
package org.jahia.modules.rolesmanager;

import org.springframework.context.i18n.LocaleContextHolder;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class RoleBean implements Serializable {

    private String uuid;

    private String parentUuid;

    private String name;

    private String path;

    private Map<String, I18nRoleProperties> i18nProperties;

    private boolean hidden = false;

    private RoleType roleType;

    private Collection<NodeType> nodeTypes;

    private int depth;

    private boolean isDirty = false;

    private Map<String, Map<String, Map<String,PermissionBean>>> permissions;
    private List<RoleBean> subRoles;

//    private Map<String, List<PermissionBean>> externalPermissions;


    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Map<String, I18nRoleProperties> getI18nProperties() {
        return i18nProperties;
    }

    public void setI18nProperties(Map<String, I18nRoleProperties> i18nProperties) {
        this.i18nProperties = i18nProperties;
    }

    public String getTitle() {
        String language = LocaleContextHolder.getLocale().getLanguage();
        if (i18nProperties.containsKey(language) && i18nProperties.get(language) != null) {
            return i18nProperties.get(language).getTitle();
        } else {
            return "";
        }
    }

    public String getDescription() {
        String language = LocaleContextHolder.getLocale().getLanguage();
        if (i18nProperties.containsKey(language) && i18nProperties.get(language) != null) {
            return i18nProperties.get(language).getDescription();
        } else {
            return "";
        }
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public RoleType getRoleType() {
        return roleType;
    }

    public void setRoleType(RoleType scope) {
        this.roleType = scope;
    }

    public Collection<NodeType> getNodeTypes() {
        return nodeTypes;
    }

    public void setNodeTypes(Collection<NodeType> nodeTypes) {
        this.nodeTypes = nodeTypes;
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public boolean isDirty() {
        return isDirty;
    }

    public void setDirty(boolean dirty) {
        isDirty = dirty;
    }

    public  Map<String, Map<String, Map<String,PermissionBean>>> getPermissions() {
        return permissions;
    }

    public void setPermissions( Map<String, Map<String, Map<String,PermissionBean>>> permissions) {
        this.permissions = permissions;
    }

    public void setSubRoles(List<RoleBean> subRoles) {
        this.subRoles = subRoles;
    }

    public List<RoleBean> getSubRoles() {
        return subRoles;
    }

    public String getParentUuid() {
        return parentUuid;
    }

    public void setParentUuid(String parentUuid) {
        this.parentUuid = parentUuid;
    }
}
