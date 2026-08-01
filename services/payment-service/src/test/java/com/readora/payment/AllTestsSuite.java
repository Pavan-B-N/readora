package com.readora.payment;

import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

/**
 * Runs every test in this service in one go — right-click and Run in the IDE, or
 * {@code mvn test -Dtest=AllTestsSuite}. Not picked up by a plain {@code mvn test} itself
 * (Surefire's default include pattern doesn't match this class name), which avoids every test
 * running twice — once individually, once again through this suite.
 */
@Suite
@SelectPackages("com.readora.payment")
public class AllTestsSuite {
}
