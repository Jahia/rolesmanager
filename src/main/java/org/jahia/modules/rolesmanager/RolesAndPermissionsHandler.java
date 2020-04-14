/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2020 Jahia Solutions Group SA. All rights reserved.
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

import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.util.ISO9075;
import org.jahia.api.Constants;
import org.jahia.data.templates.JahiaTemplatesPackage;
import org.jahia.services.content.*;
import org.jahia.services.content.nodetypes.ExtendedNodeType;
import org.jahia.services.content.nodetypes.NodeTypeRegistry;
import org.jahia.services.templates.JahiaTemplateManagerService;
import org.jahia.utils.Patterns;
import org.jahia.utils.i18n.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.binding.message.MessageBuilder;
import org.springframework.binding.message.MessageContext;
import org.springframework.context.i18n.LocaleContextHolder;

import javax.jcr.*;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import java.io.Serializable;
import java.util.*;
import java.util.regex.Pattern;

public class RolesAndPermissionsHandler implements Serializable {

    private static final long serialVersionUID = 7910715831938629654L;

    private static final Logger logger = LoggerFactory.getLogger(RolesAndPermissionsHandler.class);
    
    private static final Pattern PATTERN_UNDERSCORE_DASH = Pattern.compile("[_-]");

    private static final Pattern PATTERN_UPPERCASE_LETTER = Pattern.compile("([A-Z])");

    private static final String OTHER_PERMISSIONS_GROUP_MAME= "other";

    @Autowired
    private transient RoleTypeConfiguration roleTypes;

    @Autowired
    private transient JahiaTemplateManagerService templateManagerService;

    private RoleBean roleBean = new RoleBean();

    private String currentContext;
    private String currentGroup;
    private List<String> uuids;

    private transient List<JCRNodeWrapper> allPermissions;

    public RolesAndPermissionsHandler() {
    }

    public RoleTypeConfiguration getRoleTypes() {
        return roleTypes;
    }

    public RoleBean getRoleBean() {
        return roleBean;
    }

    public void setRoleBean(RoleBean roleBean) {
        this.roleBean = roleBean;
        this.currentContext = "current";
        this.currentGroup = roleBean.getPermissions() != null ? roleBean.getPermissions().get(currentContext).keySet().iterator().next() : null;
    }

    private JCRSessionWrapper getSession() throws RepositoryException {
        return getSession(LocaleContextHolder.getLocale());
    }

    private JCRSessionWrapper getSession(Locale locale) throws RepositoryException {
        return JCRSessionFactory.getInstance().getCurrentUserSession("default", locale);
    }

    public Map<String, List<RoleBean>> getRoles() throws RepositoryException {
        return getRoles(false, false);
    }

    public Map<String, List<RoleBean>> getRolesToDelete() throws RepositoryException {
        return getRoles(true, true);
    }


    public Map<String, List<RoleBean>> getSelectedRoles() throws RepositoryException {
        return getRoles(true, false);
    }

    public Map<String, List<RoleBean>> getRoles(boolean filterUUIDs, boolean getChildren) throws RepositoryException {

        QueryManager qm = getSession().getWorkspace().getQueryManager();
        StringBuilder statement = new StringBuilder("select * from [jnt:role] as role");
        if (filterUUIDs) {
            statement.append(" where ");
            Iterator<String> it = uuids.iterator();
            while (it.hasNext()) {
                String uuid = it.next();
                statement.append("[jcr:uuid] = '").append(uuid).append("'");
                if (getChildren) {
                    statement.append(" or isdescendantnode(role, ['").append(getSession().getNodeByIdentifier(uuid).getPath()).append("'])");
                }
                if (it.hasNext()) {
                    statement.append(" or ");
                }
            }
        }
        Query q = qm.createQuery(statement.toString(), Query.JCR_SQL2);
        Map<String, List<RoleBean>> all = new LinkedHashMap<String, List<RoleBean>>();
        if (!filterUUIDs) {
            for (RoleType roleType : roleTypes.getValues()) {
                all.put(roleType.getName(), new ArrayList<RoleBean>());
            }
        }

        NodeIterator ni = q.execute().getNodes();
        while (ni.hasNext()) {
            JCRNodeWrapper next = (JCRNodeWrapper) ni.next();
            if (!next.getName().equals("privileged")) {
                RoleBean role = createRoleBean(next, false, false);
                String key = role.getRoleType().getName();
                if (!all.containsKey(key)) {
                    all.put(key, new ArrayList<RoleBean>());
                }
                all.get(key).add(role);
            }
        }
        for (List<RoleBean> roleBeans : all.values()) {
            Collections.sort(roleBeans, new Comparator<RoleBean>() {
                @Override
                public int compare(RoleBean role1, RoleBean role2) {
                    String[] path1 = role1.getPathElements();
                    String[] path2 = role2.getPathElements();
                    for (int i = 0; true; i++) {
                        if (i == path1.length && i == path2.length) {
                            return 0;
                        } else if (i == path1.length) {
                            return -1;
                        } else if (i == path2.length) {
                            return 1;
                        }
                        int result = path1[i].compareTo(path2[i]);
                        if (result != 0) {
                            return result;
                        }
                    }
                }
            });
        }

        return all;
    }

    public RoleBean getRole(String uuid, boolean getPermissions) throws RepositoryException {
        JCRSessionWrapper currentUserSession = getSession();

        JCRNodeWrapper role = currentUserSession.getNodeByIdentifier(uuid);

        return createRoleBean(role, getPermissions, true);
    }

    public boolean copyRole(final String roleName, final String deepCopy, final String uuid, final MessageContext messageContext) throws RepositoryException {
        final JCRSessionWrapper currentUserSession = getSession();
        boolean copy = JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(currentUserSession.getUser(), currentUserSession.getWorkspace().getName(), null, new JCRCallback<Boolean>() {
            @Override
            public Boolean doInJCR(JCRSessionWrapper session) throws RepositoryException {
                JCRNodeWrapper roleToCopy = session.getNodeByIdentifier(uuid);

                String newRoleName = StringUtils.isNotEmpty(roleName) ? JCRContentUtils.generateNodeName(roleName) : roleName;
                if (!testRoleName(newRoleName, messageContext, session)) {
                    return false;
                }

                boolean copy = roleToCopy.copy(roleToCopy.getParent().getPath(), newRoleName);
                JCRNodeWrapper copiedNode = session.getNode(roleToCopy.getParent().getPath() + "/" + newRoleName);
                NodeIterator iterator = copiedNode.getNodes();
                boolean copyWithSubRoles = StringUtils.isNotEmpty(deepCopy);
                while (iterator.hasNext()) {
                    JCRNodeWrapper subNode = (JCRNodeWrapper) iterator.next();
                    if (subNode.isNodeType("jnt:role")) {
                        if (copyWithSubRoles) {
                            renameSubRole(subNode, newRoleName);
                        } else {
                            subNode.remove();
                        }
                    }
                }

                session.save();

                if (copy) {
                    messageContext.addMessage(new MessageBuilder().source("roleName")
                            .info()
                            .code("rolesmanager.rolesAndPermissions.successfullyCopied")
                            .args(roleToCopy.getName(), newRoleName)
                            .build());
                }

                return copy;
            }
        });

        return copy;
    }

    protected void renameSubRole(JCRNodeWrapper subNode, String newRoleName) throws RepositoryException {
        String newSubRoleName = newRoleName + '-' + subNode.getName();
        boolean exists = roleExists(newSubRoleName, subNode.getSession());
        int i = 2;
        while (exists) {
            newSubRoleName = newRoleName + '-' + subNode.getName() + '-' + i;
            exists = roleExists(newSubRoleName, subNode.getSession());
            i++;
        }
        subNode.rename(newSubRoleName);
    }

    private RoleBean createRoleBean(JCRNodeWrapper role, boolean getPermissions, boolean getSubRoles) throws RepositoryException {
        RoleBean roleBean = new RoleBean();
        JCRNodeWrapper parentRole = JCRContentUtils.getParentOfType(role, "jnt:role");
        final String uuid = role.getIdentifier();
        roleBean.setUuid(uuid);
        roleBean.setParentUuid(parentRole != null ? parentRole.getIdentifier() : null);
        roleBean.setName(role.getName());
        roleBean.setPath(role.getPath());
        roleBean.setDepth(role.getDepth());

        JCRNodeWrapper n;
        Map<String, I18nRoleProperties> i18nRoleProperties = new TreeMap<String, I18nRoleProperties>();
        for (Locale l : role.getExistingLocales()) {
            n = getSession(l).getNodeByIdentifier(uuid);
            if (!n.hasProperty(Constants.JCR_TITLE) && !n.hasProperty(Constants.JCR_DESCRIPTION)) {
                i18nRoleProperties.put(l.getLanguage(), null);
                continue;
            }
            I18nRoleProperties properties = new I18nRoleProperties();
            properties.setLanguage(l.getDisplayName(LocaleContextHolder.getLocale()));
            if (n.hasProperty(Constants.JCR_TITLE)) {
                properties.setTitle(n.getProperty(Constants.JCR_TITLE).getString());
            }
            if (n.hasProperty(Constants.JCR_DESCRIPTION)) {
                properties.setDescription(n.getProperty(Constants.JCR_DESCRIPTION).getString());
            }
            i18nRoleProperties.put(l.getLanguage(), properties);
        }
        roleBean.setI18nProperties(i18nRoleProperties);

        if (role.hasProperty("j:hidden")) {
            roleBean.setHidden(role.getProperty("j:hidden").getBoolean());
        }

        String roleGroup = "edit-role";
        if (role.hasProperty("j:roleGroup")) {
            roleGroup = role.getProperty("j:roleGroup").getString();
        }

        RoleType roleType = roleTypes.get(roleGroup);
        roleBean.setRoleType(roleType);
        if (getPermissions) {
            List<String> tabs = new ArrayList<String>(roleBean.getRoleType().getScopes());

            Map<String, List<String>> permIdsMap = new HashMap<String, List<String>>();
            fillPermIds(role, tabs, permIdsMap, false);

            Map<String, List<String>> inheritedPermIdsMap = new HashMap<String, List<String>>();
            fillPermIds(role.getParent(), tabs, inheritedPermIdsMap, true);


            Map<String, Map<String, Map<String, PermissionBean>>> permsForRole = new LinkedHashMap<String, Map<String, Map<String, PermissionBean>>>();
            roleBean.setPermissions(permsForRole);

            for (String tab : tabs) {
                addPermissionsForScope(roleBean, tab, permIdsMap, inheritedPermIdsMap);
            }

            if (roleType.getAvailableNodeTypes() != null) {
                List<String> nodeTypesOnRole = new ArrayList<String>();
                if (role.hasProperty("j:nodeTypes")) {
                    for (Value value : role.getProperty("j:nodeTypes").getValues()) {
                        nodeTypesOnRole.add(value.getString());
                    }
                }


                SortedSet<NodeType> nodeTypes = new TreeSet<NodeType>();
                for (String s : roleType.getAvailableNodeTypes()) {
                    boolean includeSubtypes = false;
                    if (s.endsWith("/*")) {
                        s = StringUtils.substringBeforeLast(s, "/*");
                        includeSubtypes = true;
                    }
                    ExtendedNodeType t = NodeTypeRegistry.getInstance().getNodeType(s);
                    nodeTypes.add(new NodeType(t.getName(), t.getLabel(LocaleContextHolder.getLocale()), nodeTypesOnRole.contains(t.getName())));
                    if (includeSubtypes) {
                        for (ExtendedNodeType sub : t.getSubtypesAsList()) {
                            nodeTypes.add(new NodeType(sub.getName(), sub.getLabel(LocaleContextHolder.getLocale()), nodeTypesOnRole.contains(sub.getName())));
                        }
                    }
                }
                roleBean.setNodeTypes(nodeTypes);
            }
        }

        // sub-roles
        if (getSubRoles) {
            final List<JCRNodeWrapper> subRoles = JCRContentUtils.getNodes(role, "jnt:role");
            final List<RoleBean> subRoleBeans = new ArrayList<RoleBean>(subRoles.size());
            for (JCRNodeWrapper subRole : subRoles) {
                subRoleBeans.add(createRoleBean(subRole, false, false));
            }
            roleBean.setSubRoles(subRoleBeans);
        }

        return roleBean;
    }

    public void revertRole() throws RepositoryException {
        roleBean = getRole(roleBean.getUuid(), true);
    }

    private void fillPermIds(JCRNodeWrapper role, List<String> tabs, Map<String, List<String>> permIdsMap, boolean recursive) throws RepositoryException {
        if (!role.isNodeType(Constants.JAHIANT_ROLE)) {
            return;
        }

        if (recursive) {
            fillPermIds(role.getParent(), tabs, permIdsMap, true);
        }

        final ArrayList<String> setPermIds = new ArrayList<String>();
        permIdsMap.put("current", setPermIds);

        if (role.hasProperty("j:permissionNames")) {
            Value[] values = role.getProperty("j:permissionNames").getValues();
            for (Value value : values) {
                String valueString = value.getString();
                if (!setPermIds.contains(valueString)) {
                    setPermIds.add(valueString);
                }
            }
        }


        NodeIterator ni = role.getNodes();
        while (ni.hasNext()) {
            JCRNodeWrapper next = (JCRNodeWrapper) ni.next();
            if (next.isNodeType("jnt:externalPermissions")) {
                try {
                    String path = next.getProperty("j:path").getString();
                    permIdsMap.put(path, new ArrayList<String>());
                    Value[] values = next.getProperty("j:permissionNames").getValues();
                    for (Value value : values) {
                        List<String> ids = permIdsMap.get(path);
                        String valueString = value.getString();
                        if (!ids.contains(valueString)) {
                            ids.add(valueString);
                        }
                        if (!tabs.contains(path)) {
                            tabs.add(path);
                        }
                    }
                } catch (RepositoryException e) {
                    logger.error("Cannot initialize role " + next.getPath(), e);
                } catch (IllegalStateException e) {
                    logger.error("Cannot initialize role " + next.getPath(), e);
                }
            }
        }
    }

    private boolean testRoleName(String roleName, MessageContext messageContext, JCRSessionWrapper currentUserSession) throws RepositoryException {
        if (StringUtils.isEmpty(roleName)) {
            messageContext.addMessage(new MessageBuilder().source("roleName")
                    .error()
                    .code("rolesmanager.rolesAndPermissions.role.noName")
                    .build());
            return false;
        }

        if (roleExists(roleName, currentUserSession)) {
            messageContext.addMessage(new MessageBuilder().source("roleName")
                    .error()
                    .code("rolesmanager.rolesAndPermissions.role.exists")
                    .build());
            return false;
        }

        return true;
    }

    private boolean roleExists(String roleName, JCRSessionWrapper currentUserSession) throws RepositoryException {
        NodeIterator nodes = currentUserSession.getWorkspace().getQueryManager()
                .createQuery(
                        "select * from [" + Constants.JAHIANT_ROLE + "] as r where localname()='"
                                + JCRContentUtils.sqlEncode(roleName) + "' and isdescendantnode(r,['/roles'])",
                        Query.JCR_SQL2)
                .execute().getNodes();
        return nodes.hasNext();
    }

    public boolean addRole(String roleName, String parentRoleId, String roleTypeString, MessageContext messageContext) throws RepositoryException {
        JCRSessionWrapper currentUserSession = getSession();

        roleName = StringUtils.isNotEmpty(roleName) ? JCRContentUtils.generateNodeName(roleName) : roleName;
        if (!testRoleName(roleName, messageContext, currentUserSession)) {
            return false;
        }

        JCRNodeWrapper parent;
        if (StringUtils.isBlank(parentRoleId)) {
            parent = currentUserSession.getNode("/roles");
        } else {
            parent = currentUserSession.getNodeByIdentifier(parentRoleId);
        }
        JCRNodeWrapper role = parent.addNode(roleName, "jnt:role");
        RoleType roleType = roleTypes.get(roleTypeString);
        role.setProperty("j:roleGroup", roleType.getName());
        role.setProperty("j:privilegedAccess", roleType.isPrivileged());
        if (roleType.getDefaultNodeTypes() != null) {
            List<Value> values = new ArrayList<Value>();
            for (String nodeType : roleType.getDefaultNodeTypes()) {
                values.add(currentUserSession.getValueFactory().createValue(nodeType));
            }
            role.setProperty("j:nodeTypes", values.toArray(new Value[values.size()]));
        }
        role.setProperty("j:roleGroup", roleType.getName());

        currentUserSession.save();
        this.setRoleBean(createRoleBean(role, true, false));
        return true;
    }

    public void selectRoles(String uuids) throws RepositoryException {
        this.uuids = Arrays.asList(uuids.split(","));
    }

    public boolean deleteRoles() throws RepositoryException {
        JCRSessionWrapper currentUserSession = getSession();
        for (String uuid : uuids) {
            try {
                currentUserSession.getNodeByIdentifier(uuid).remove();
            } catch (ItemNotFoundException e) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Cannot find role " + uuid);
                }
            }
        }
        currentUserSession.save();
        return true;
    }

    private void addPermissionsForScope(RoleBean roleBean, String scope, Map<String, List<String>> permIdsMap, Map<String, List<String>> inheritedPermIdsMap) throws RepositoryException {
        final Map<String, Map<String, Map<String, PermissionBean>>> permissions = roleBean.getPermissions();
        if (!permissions.containsKey(scope)) {
            permissions.put(scope, new LinkedHashMap<String, Map<String, PermissionBean>>());
        }
        List<JCRNodeWrapper> allPermissions = getAllPermissions();

        String type = null;
        final Map<String, List<String>> globalPermissionsGroups = roleTypes.getPermissionsGroups();
        final Map<String, List<String>> permissionsGroupsForRoleType = roleBean.getRoleType().getPermissionsGroups();

        if (scope.equals("current")) {
            if (roleBean.getRoleType().getDefaultNodeTypes() != null && roleBean.getRoleType().getDefaultNodeTypes().size() == 1) {
                type = roleBean.getRoleType().getDefaultNodeTypes().get(0);
            }
        } else {
            if (scope.equals("currentSite")) {
                type = "jnt:virtualsite";
            } else if (scope.startsWith("/")) {
                try {
                    type = getSession().getNode(scope).getPrimaryNodeTypeName();
                } catch (PathNotFoundException e) {
                    logger.debug("Error retrieving scope", e);
                } catch (RepositoryException e) {
                    logger.error("Error retrieving scope", e);
                }
            }
        }
        if (type == null || (!globalPermissionsGroups.containsKey(type) && (permissionsGroupsForRoleType == null || !permissionsGroupsForRoleType.containsKey(type)))) {
            type = "nt:base";
        }
        if (permissionsGroupsForRoleType != null && permissionsGroupsForRoleType.containsKey(type)) {
            for (String s : permissionsGroupsForRoleType.get(type)) {
                permissions.get(scope).put(s, new TreeMap<String, PermissionBean>());
            }
        } else {
            for (String s : globalPermissionsGroups.get(type)) {
                permissions.get(scope).put(s, new TreeMap<String, PermissionBean>());
            }
        }
        // Add "other" group that will contain all permissions outside the current group list
        permissions.get(scope).put(OTHER_PERMISSIONS_GROUP_MAME, new TreeMap<String, PermissionBean>());

        Map<String, PermissionBean> mappedPermissions = new HashMap<String, PermissionBean>();

        Map<String, String> allGroups = new HashMap<String, String>();
        for (String s : permissions.get(scope).keySet()) {
            for (String s1 : Arrays.asList(s.split(","))) {
                allGroups.put(s1, s);
            }
        }

        // Create mapped permissions
        for (Map.Entry<String, List<String>> entry : roleTypes.getPermissionsMapping().entrySet()) {
            String[] splitPath = entry.getKey().split("/");
            String permissionGroup = splitPath[2];
            String groupKey = getGroupKey(allGroups, permissionGroup);
            Map<String, PermissionBean> p = permissions.get(scope).get(groupKey);
            PermissionBean bean = new PermissionBean();
            bean.setUuid(null);
            bean.setParentPath(StringUtils.substringBeforeLast(entry.getKey(), "/"));
            bean.setName(StringUtils.substringAfterLast(entry.getKey(), "/"));
            populateTitleAndDescription(bean);
            bean.setPath(entry.getKey());
            bean.setDepth(splitPath.length - 1);
            bean.setMappedPermissions(new TreeMap<String, PermissionBean>());
            if (p.containsKey(bean.getParentPath())) {
                p.get(bean.getParentPath()).setHasChildren(true);
            }

            p.put(entry.getKey(), bean);

            for (String s : entry.getValue()) {
                createMappedPermission(s, bean, mappedPermissions);
            }
        }

        // Create standard permissions
        for (JCRNodeWrapper permissionNode : allPermissions) {
            JCRNodeWrapper permissionGroup = getPermissionGroupNode(permissionNode);
            final String permissionPath = getPermissionPath(permissionNode);

            if (!mappedPermissions.containsKey(permissionPath) && mappedPermissions.containsKey(getPermissionPath(permissionNode.getParent()))) {
                final PermissionBean bean = mappedPermissions.get(getPermissionPath(permissionNode.getParent()));
                createMappedPermission(permissionPath, bean, mappedPermissions);
            }

            if (!mappedPermissions.containsKey(permissionPath)) {
                String groupKey = getGroupKey(allGroups, permissionGroup.getName());
                Map<String, PermissionBean> p = permissions.get(scope).get(groupKey);
                if (!p.containsKey(permissionPath) || permissionNode.getPath().startsWith("/permissions")) {
                    PermissionBean bean = new PermissionBean();
                    setPermissionBeanProperties(permissionNode, bean);
                    if (p.containsKey(bean.getParentPath())) {
                        p.get(bean.getParentPath()).setHasChildren(true);
                    }
                    p.put(permissionPath, bean);
                    setPermissionFlags(permissionNode, p, bean, permIdsMap.get(scope), inheritedPermIdsMap.get(scope), p.get(bean.getParentPath()));
                }
            }
            if (mappedPermissions.containsKey(permissionPath)) {
                PermissionBean bean = mappedPermissions.get(permissionPath);
                String groupKey = getGroupKey(allGroups, bean.getPath().split("/")[2]);
                Map<String, PermissionBean> p = permissions.get(scope).get(groupKey);
                setPermissionFlags(permissionNode, p, bean, permIdsMap.get(scope), inheritedPermIdsMap.get(scope), p.get(bean.getParentPath()));
            }
        }

        // Auto expand permissions where mapped permissions are partially set
        for (Map<String, Map<String, PermissionBean>> map : roleBean.getPermissions().values()) {
            for (Map<String, PermissionBean> map2 : map.values()) {
                final Collection<PermissionBean> values = new ArrayList<PermissionBean>(map2.values());
                for (PermissionBean bean : values) {
                    if (bean.getMappedPermissions() != null) {
                        Boolean lastValue = null;
                        for (PermissionBean value : bean.getMappedPermissions().values()) {
                            if (lastValue == null) {
                                lastValue = value.isSuperSet() || value.isSet();
                            }
                            if (!lastValue.equals(value.isSuperSet() || value.isSet())) {
                                bean.setMappedPermissionsExpanded(true);
                                bean.setSet(false);
                                bean.setSuperSet(false);
                                bean.setPartialSet(true);
                                break;
                            }
                        }
                        if (bean.isMappedPermissionsExpanded()) {
                            for (PermissionBean mapped : bean.getMappedPermissions().values()) {
                                map2.put(mapped.getPath(), mapped);
                            }
                        }
                    }
                }
            }
        }
    }

    private String getGroupKey(Map<String, String> allGroups, String permissionGroup) {
        return StringUtils.defaultString(allGroups.get(permissionGroup), OTHER_PERMISSIONS_GROUP_MAME);
    }

    private void createMappedPermission(String permissionPath, PermissionBean parent, Map<String, PermissionBean> mappedPermissions) throws RepositoryException {
        PermissionBean mapped = new PermissionBean();
        setPermissionBeanProperties(getSession().getNode(permissionPath), mapped);
        mapped.setPath(parent.getPath() + "/" + mapped.getName());
        mapped.setParentPath(parent.getPath());
        mapped.setDepth(parent.getDepth() + 1);
        parent.getMappedPermissions().put(permissionPath, mapped);
        mappedPermissions.put(permissionPath, parent);
    }

    private void setPermissionFlags(JCRNodeWrapper permissionNode, Map<String, PermissionBean> permissions, PermissionBean bean, List<String> permIds, List<String> inheritedPermIds, PermissionBean parentBean) throws RepositoryException {
        if ((permIds != null && permIds.contains(permissionNode.getName()))
                || (parentBean != null && parentBean.isSet())) {
            bean.setSet(true);
            if (bean.getMappedPermissions() != null && bean.getMappedPermissions().containsKey(permissionNode.getPath())) {
                bean.getMappedPermissions().get(permissionNode.getPath()).setSet(true);
            }
            while (parentBean != null && !parentBean.isSet() && !parentBean.isSuperSet()) {
                parentBean.setPartialSet(true);
                parentBean = permissions.get(parentBean.getParentPath());
            }
        }
        parentBean = permissions.get(bean.getParentPath());
        if ((inheritedPermIds != null && inheritedPermIds.contains(permissionNode.getName()))
                || (parentBean != null && parentBean.isSuperSet())) {
            bean.setSuperSet(true);
            if (bean.getMappedPermissions() != null && bean.getMappedPermissions().containsKey(permissionNode.getPath())) {
                bean.getMappedPermissions().get(permissionNode.getPath()).setSuperSet(true);
            }
            while (parentBean != null && !parentBean.isSet() && !parentBean.isSuperSet()) {
                parentBean.setPartialSet(true);
                parentBean = permissions.get(parentBean.getParentPath());
            }
        }
    }

    private String getPermissionPath(JCRNodeWrapper permissionNode) {
        String path = permissionNode.getPath();
        if (path.startsWith("/modules")) {
            path = "/permissions/" + StringUtils.substringAfter(path, "/permissions/");
        }
        return path;
    }

    private String getPermissionModule(JCRNodeWrapper permissionNode) {
        String path = permissionNode.getPath();
        if (path.startsWith("/modules/")) {
            String s = StringUtils.substringAfter(path, "/modules/");
            return StringUtils.substringBefore(s, "/");
        }
        return null;
    }

    private int getPermissionDepth(JCRNodeWrapper permissionNode) throws RepositoryException {
        String path = permissionNode.getPath();
        if (path.startsWith("/modules")) {
            return permissionNode.getDepth() - 3;
        }
        return permissionNode.getDepth();
    }

    private JCRNodeWrapper getPermissionGroupNode(JCRNodeWrapper permissionNode) throws RepositoryException {
        JCRNodeWrapper permissionGroup = (JCRNodeWrapper) permissionNode.getAncestor(2);
        if (permissionGroup.isNodeType("jnt:module")) {
            permissionGroup = (JCRNodeWrapper) permissionNode.getAncestor(5);
        }
        return permissionGroup;
    }

    private void setPermissionBeanProperties(JCRNodeWrapper permissionNode, PermissionBean bean) throws RepositoryException {
        final String module = getPermissionModule(permissionNode);

        bean.setUuid(permissionNode.getIdentifier());

        bean.setParentPath(getPermissionPath(permissionNode.getParent()));
        bean.setName(permissionNode.getName());
        bean.setModule(module);
        populateTitleAndDescription(bean);
        bean.setPath(getPermissionPath(permissionNode));
        bean.setDepth(getPermissionDepth(permissionNode));
    }

    private void populateTitleAndDescription(PermissionBean bean) {
        String localName = bean.getName().contains(":") ? StringUtils.substringAfter(bean.getName(), ":") : bean.getName(); 
        if (localName.contains(":")) {
            localName = StringUtils.substringAfter(localName, ":");
        }
        String title = StringUtils.capitalize(PATTERN_UNDERSCORE_DASH.matcher(PATTERN_UPPERCASE_LETTER.matcher(localName).replaceAll(" $0")).replaceAll(" ").toLowerCase());
        final String rbName = Patterns.DASH.matcher(localName).replaceAll("_");
        Locale locale = LocaleContextHolder.getLocale();
        String rbKey = "label.permission." + rbName;
        String internalTitle = Messages.getInternal(rbKey, locale, title);
        if (bean.getModule() != null) {
            bean.setTitle(Messages.get(templateManagerService.getTemplatePackageById(bean.getModule()), rbKey, locale, internalTitle));
        } else {
            bean.setTitle(internalTitle);
        }
        String internalDescription = Messages.getInternal(rbKey + ".description", locale, "");
        if (bean.getModule() != null) {
            bean.setDescription(Messages.get(templateManagerService.getTemplatePackageById(bean.getModule()), rbKey + ".description", locale, internalDescription));
        } else {
            bean.setDescription(internalDescription);
        }
    }

    public String getCurrentContext() {
        return currentContext;
    }

    public void setCurrentContext(String tab) {
        currentContext = tab;
        this.currentGroup = roleBean.getPermissions().get(currentContext).keySet().iterator().next();
    }

    public String getCurrentGroup() {
        return currentGroup;
    }

    public void setCurrentGroup(String context, String currentGroup) {
        setCurrentContext(context);
        this.currentGroup = currentGroup;
    }

    public void storeValues(String[] selectedValues, String[] partialSelectedValues) {
        Map<String, PermissionBean> permissionBeans = roleBean.getPermissions().get(currentContext).get(currentGroup);
        List<String> perms = selectedValues != null ? Arrays.asList(selectedValues) : new ArrayList<String>();
        for (PermissionBean permissionBean : permissionBeans.values()) {
            if (permissionBean.isSet() != perms.contains(permissionBean.getPath())) {
                roleBean.setDirty(true);
                permissionBean.setSet(perms.contains(permissionBean.getPath()));
                if (!permissionBean.isMappedPermissionsExpanded() && permissionBean.getMappedPermissions() != null) {
                    for (PermissionBean mapped : permissionBean.getMappedPermissions().values()) {
                        mapped.setSet(perms.contains(permissionBean.getPath()));
                    }
                }
            }

        }

        perms = partialSelectedValues != null ? Arrays.asList(partialSelectedValues) : new ArrayList<String>();
        for (PermissionBean permissionBean : permissionBeans.values()) {
            if (permissionBean.isPartialSet() != perms.contains(permissionBean.getPath())) {
                roleBean.setDirty(true);
                permissionBean.setPartialSet(perms.contains(permissionBean.getPath()));
                if (!permissionBean.isMappedPermissionsExpanded() && permissionBean.getMappedPermissions() != null) {
                    for (PermissionBean mapped : permissionBean.getMappedPermissions().values()) {
                        mapped.setPartialSet(perms.contains(permissionBean.getPath()));
                    }
                }
            }
        }
    }

    public void addContext(String newContext) throws RepositoryException {
        if (!newContext.startsWith("/")) {
            return;
        }

        if (!roleBean.getPermissions().containsKey(newContext)) {
            addPermissionsForScope(roleBean, newContext, new HashMap<String, List<String>>(), new HashMap<String, List<String>>());
        }
        setCurrentContext(newContext);
    }

    public void removeContext(String scope) throws RepositoryException {

        if (roleBean.getPermissions().containsKey(scope)) {
            roleBean.getPermissions().remove(scope);
        }
        if (currentContext.equals(scope)) {
            setCurrentContext(roleBean.getPermissions().keySet().iterator().next());
        }
    }

    public void save() throws RepositoryException {
        JCRSessionWrapper currentUserSession = getSession();

        Map<String, List<Value>> permissions = new HashMap<String, List<Value>>();

        for (Map.Entry<String, Map<String, Map<String, PermissionBean>>> entry : roleBean.getPermissions().entrySet()) {
            ArrayList<Value> permissionValues = new ArrayList<Value>();
            permissions.put(entry.getKey(), permissionValues);
            for (Map<String, PermissionBean> map : entry.getValue().values()) {
                for (PermissionBean bean : map.values()) {
                    PermissionBean parentBean = map.get(bean.getParentPath());
                    if (bean.isSet() && (parentBean == null || !parentBean.isSet())) {
                        if (bean.getMappedPermissions() != null) {
                            for (PermissionBean mapped : bean.getMappedPermissions().values()) {
                                if (mapped.isSet()) {
                                    permissionValues.add(currentUserSession.getValueFactory().createValue(mapped.getName(), PropertyType.STRING));
                                }
                            }
                        } else {
                            permissionValues.add(currentUserSession.getValueFactory().createValue(bean.getName(), PropertyType.STRING));
                        }
                    }
                }
            }
        }

        JCRNodeWrapper role = currentUserSession.getNodeByIdentifier(roleBean.getUuid());
        Set<String> externalPermissionNodes = new HashSet<String>();
        for (Map.Entry<String, List<Value>> s : permissions.entrySet()) {
            String key = s.getKey();
            if (key.equals("current")) {
                role.setProperty("j:permissionNames", permissions.get("current").toArray(new Value[permissions.get("current").size()]));
            } else {
                if (key.equals("/")) {
                    key = "root-access";
                } else {
                    key = ISO9075.encode((key.startsWith("/") ? key.substring(1) : key).replace("/", "-")) + "-access";
                }
                if (!s.getValue().isEmpty()) {
                    if (!role.hasNode(key)) {
                        JCRNodeWrapper extPermissions = role.addNode(key, "jnt:externalPermissions");
                        extPermissions.setProperty("j:path", s.getKey());
                        extPermissions.setProperty("j:permissionNames", s.getValue().toArray(new Value[s.getValue().size()]));
                    } else {
                        role.getNode(key).setProperty("j:permissionNames", s.getValue().toArray(new Value[s.getValue().size()]));
                    }
                    externalPermissionNodes.add(key);
                }
            }
        }
        NodeIterator ni = role.getNodes();
        while (ni.hasNext()) {
            JCRNodeWrapper next = (JCRNodeWrapper) ni.next();
            if (next.getPrimaryNodeTypeName().equals("jnt:externalPermissions") && !externalPermissionNodes.contains(next.getName())) {
                next.remove();
            }
        }

        JCRNodeWrapper n;
        Map<String, I18nRoleProperties> i18nRoleProperties = roleBean.getI18nProperties();
        for (String l : i18nRoleProperties.keySet()) {
            n = getSession(new Locale(l)).getNodeByIdentifier(roleBean.getUuid());
            I18nRoleProperties properties = i18nRoleProperties.get(l);
            if (properties == null) {
                if (n.hasProperty(Constants.JCR_TITLE)) {
                    n.getProperty(Constants.JCR_TITLE).remove();
                }
                if (n.hasProperty(Constants.JCR_DESCRIPTION)) {
                    n.getProperty(Constants.JCR_DESCRIPTION).remove();
                }
            } else {
                String title = properties.getTitle();
                if (StringUtils.isNotBlank(title)) {
                    n.setProperty(Constants.JCR_TITLE, title);
                } else if (n.hasProperty(Constants.JCR_TITLE)) {
                    n.getProperty(Constants.JCR_TITLE).remove();
                }
                String description = properties.getDescription();
                if (StringUtils.isNotBlank(description)) {
                    n.setProperty(Constants.JCR_DESCRIPTION, description);
                } else if (n.hasProperty(Constants.JCR_DESCRIPTION)) {
                    n.getProperty(Constants.JCR_DESCRIPTION).remove();
                }
            }
        }

        role.setProperty("j:hidden", roleBean.isHidden());
        if (roleBean.getNodeTypes() != null) {
            List<Value> values = new ArrayList<Value>();
            for (NodeType nodeType : roleBean.getNodeTypes()) {
                if (nodeType.isSet()) {
                    values.add(currentUserSession.getValueFactory().createValue(nodeType.getName()));
                }
            }
            role.setProperty("j:nodeTypes", values.toArray(new Value[values.size()]));
        }
        roleBean.setDirty(false);
        currentUserSession.save();
    }

    public List<JCRNodeWrapper> getAllPermissions() throws RepositoryException {
        if (allPermissions != null) {
            return allPermissions;
        }

        allPermissions = new ArrayList<JCRNodeWrapper>();
        JCRSessionWrapper currentUserSession = getSession();

        QueryManager qm = currentUserSession.getWorkspace().getQueryManager();
        String statement = "select * from [jnt:permission]";

        Query q = qm.createQuery(statement, Query.JCR_SQL2);
        NodeIterator ni = q.execute().getNodes();
        while (ni.hasNext()) {
            JCRNodeWrapper next = (JCRNodeWrapper) ni.next();
            int depth = 2;
            if (((JCRNodeWrapper) next.getAncestor(1)).isNodeType("jnt:modules")) {
                depth = 5;
                JahiaTemplatesPackage pack = templateManagerService.getTemplatePackageById(next.getAncestor(2).getName());
                if (pack == null || !pack.getVersion().toString().equals(next.getAncestor(3).getName())) {
                    continue;
                }
            }
            if (next.getDepth() >= depth) {
                allPermissions.add(next);
            }
        }
        Collections.sort(allPermissions, new Comparator<JCRNodeWrapper>() {
            @Override
            public int compare(JCRNodeWrapper o1, JCRNodeWrapper o2) {
                if (getPermissionPath(o1).equals(getPermissionPath(o2))) {
                    return o2.getPath().compareTo(o1.getPath());
                }
                return getPermissionPath(o1).compareTo(getPermissionPath(o2));
            }
        });

        return allPermissions;
    }

    public void storeDetails(String[] languageCodes, String[] languageNames, String[] titles, String[] descriptions, Boolean hidden, String[] nodeTypes) {
        if (languageCodes != null) {
            Map<String, I18nRoleProperties> i18nProperties = roleBean.getI18nProperties();
            for (int i = 0; i < languageCodes.length; i++) {
                String l = languageCodes[i];
                String title;
                if (titles != null && languageCodes.length == 1) {
                    title = StringUtils.join(titles, ", ");
                } else if (titles != null && i < titles.length) {
                    title = titles[i];
                } else {
                    title = "";
                }
                String description;
                if (descriptions != null && languageCodes.length == 1) {
                    description = StringUtils.join(descriptions, ", ");
                } else if (descriptions != null && i < descriptions.length) {
                    description = descriptions[i];
                } else {
                    description = "";
                }
                if (StringUtils.isBlank(title) && StringUtils.isBlank(description)) {
                    i18nProperties.put(l, null);
                    continue;
                }
                I18nRoleProperties properties;
                if (!i18nProperties.containsKey(l) || i18nProperties.get(l) == null) {
                    properties = new I18nRoleProperties();
                    properties.setLanguage(languageNames[i]);
                    i18nProperties.put(l, properties);
                } else {
                    properties = i18nProperties.get(l);
                }
                if (!title.equals(properties.getTitle())) {
                    roleBean.setDirty(true);
                    properties.setTitle(title);
                }
                if (!description.equals(properties.getDescription())) {
                    roleBean.setDirty(true);
                    properties.setDescription(description);
                }
            }
        }

        roleBean.setHidden(hidden != null && hidden);

        if (roleBean.getNodeTypes() != null) {
            if (nodeTypes != null) {
                List<String> values = Arrays.asList(nodeTypes);
                for (NodeType nodeType : roleBean.getNodeTypes()) {
                    nodeType.setSet(values.contains(nodeType.getName()));
                }
            } else {
                for (NodeType nodeType : roleBean.getNodeTypes()) {
                    nodeType.setSet(false);
                }
            }
        }
    }

    public void expandMapped(String path) {
        Map<String, PermissionBean> permissionBeans = roleBean.getPermissions().get(currentContext).get(currentGroup);
        final PermissionBean permissionBean = permissionBeans.get(path);
        if (permissionBean != null && !permissionBean.isPartialSet()) {
            if (!permissionBean.isMappedPermissionsExpanded()) {
                permissionBean.setMappedPermissionsExpanded(true);
                for (PermissionBean mapped : permissionBean.getMappedPermissions().values()) {
                    permissionBeans.put(mapped.getPath(), mapped);
                }
            } else {
                permissionBean.setMappedPermissionsExpanded(false);
                for (PermissionBean mapped : permissionBean.getMappedPermissions().values()) {
                    permissionBeans.remove(mapped.getPath());
                }
            }

        }
    }

    public Map<String, String> getAvailableLanguages() throws RepositoryException {
        Set<String> languages = new TreeSet<String>(getSession().getNodeByIdentifier(roleBean.getUuid()).getResolveSite().getLanguages());
        Map<String, I18nRoleProperties> i18nProperties = roleBean.getI18nProperties();
        for (String l : i18nProperties.keySet()) {
            if (i18nProperties.get(l) != null) {
                languages.remove(l);
            }
        }

        TreeMap<String, String> availableLanguages = new TreeMap<String, String>();
        for (String l : languages) {
            Locale locale = new Locale(l);
            availableLanguages.put(l, locale.getDisplayName(LocaleContextHolder.getLocale()));
        }
        return availableLanguages;
    }
}
