import registrations from './registrations';
import {registry} from '@jahia/ui-extender';
import i18next from 'i18next';

export default function () {
    registry.add('callback', 'rolesmanager', {
        targets: ['jahiaApp-init:50'],
        callback: () => {
            i18next.loadNamespaces('rolesmanager');
            registrations();
            console.log('%c Roles manager routes have been registered', 'color: #3c8cba');
        }
    });
}
