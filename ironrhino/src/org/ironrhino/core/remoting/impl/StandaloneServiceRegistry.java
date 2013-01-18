package org.ironrhino.core.remoting.impl;

import static org.ironrhino.core.metadata.Profiles.DEFAULT;

import javax.inject.Named;
import javax.inject.Singleton;

import org.springframework.context.annotation.Profile;

@Singleton
@Named("serviceRegistry")
@Profile(DEFAULT)
public class StandaloneServiceRegistry extends AbstractServiceRegistry {

}
