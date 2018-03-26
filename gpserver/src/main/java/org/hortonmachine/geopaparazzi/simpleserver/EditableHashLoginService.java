package org.hortonmachine.geopaparazzi.simpleserver;

import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;

import org.eclipse.jetty.security.AbstractLoginService;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.util.security.Credential;

public class EditableHashLoginService extends AbstractLoginService {
    public static final String DEFAULT_ROLE = "defaultrole";
    public static final String[] DEFAULT_ROLES = new String[]{DEFAULT_ROLE};
    protected final Map<String, UserIdentity> _knownUserIdentities = new HashMap<String, UserIdentity>();

    public void addUser( String user, String password ) {
        if (!isRunning()) {
            Credential credential = Credential.getCredential(password);
            Principal userPrincipal = new AbstractLoginService.UserPrincipal(user, credential);
            Subject subject = new Subject();
            subject.getPrincipals().add(userPrincipal);
            subject.getPrivateCredentials().add(credential);
            subject.getPrincipals().add(new AbstractLoginService.RolePrincipal(DEFAULT_ROLE));
            subject.setReadOnly();
            _knownUserIdentities.put(user, _identityService.newUserIdentity(subject, userPrincipal, DEFAULT_ROLES));
        }
    }

    @Override
    protected String[] loadRoleInfo( UserPrincipal user ) {
        UserIdentity id = _knownUserIdentities.get(user.getName());
        if (id == null)
            return null;

        Set<RolePrincipal> roles = id.getSubject().getPrincipals(RolePrincipal.class);
        if (roles == null)
            return null;

        List<String> list = new ArrayList<>();
        for( RolePrincipal r : roles )
            list.add(r.getName());

        return list.toArray(new String[roles.size()]);
    }

    @Override
    protected UserPrincipal loadUserInfo( String username ) {
        UserIdentity id = _knownUserIdentities.get(username);
        if (id != null) {
            return (UserPrincipal) id.getUserPrincipal();
        }
        return null;
    }

}
