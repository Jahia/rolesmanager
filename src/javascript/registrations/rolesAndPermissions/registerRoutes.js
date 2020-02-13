import {registry} from '@jahia/ui-extender';

export const registerRoutes = function () {
    const level = 'server';
    const parentTarget = 'administration-server';

    const path = '/administration/rolesAndPermissions';
    const route = 'rolesAndPermissions';
    registry.addOrReplace('adminRoute', `${level}-${path.toLowerCase()}`, {
        id: route,
        targets: [`${parentTarget}-usersandroles:6`],
        path: path,
        route: route,
        defaultPath: path,
        icon: null,
        label: 'rolesmanager:rolesAndPermissions.label',
        childrenTarget: 'usersandroles',
        isSelectable: true,
        level: level
    });
};
