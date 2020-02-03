import {registerRoutes as registerRolesRoutes} from './rolesAndPermissions/registerRoutes';
import {useTranslation} from 'react-i18next';

export default function () {
    const {t} = useTranslation('rolesmanager');

    registerRolesRoutes(t);

    return null;
}
