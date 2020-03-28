import {registry} from '@jahia/ui-extender';

export const registerRoutes = function () {
    registry.addOrReplace('adminRoute', 'rolesAndPermissions', {
        targets: ['administration-server-usersAndRoles:30'],
        requiredPermission: 'adminRoles',
        icon: null,
        label: 'rolesmanager:rolesAndPermissions.label',
        isSelectable: true,
        iframeUrl: window.contextJsParameters.contextPath + '/cms/adminframe/default/en/settings.rolesAndPermissions.html'
    });
};
