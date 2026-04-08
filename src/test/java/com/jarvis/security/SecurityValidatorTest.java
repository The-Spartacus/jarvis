package com.jarvis.security;

import com.jarvis.Configuration;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for SecurityValidator
 */
public class SecurityValidatorTest {

    private SecurityValidator validator;

    @Before
    public void setUp() throws Exception {
        validator = new SecurityValidator(createMockConfig());
    }

    private Configuration createMockConfig() throws Exception {
        return new Configuration() {
            @Override
            public int getMaxCommandLength() {
                return 200;
            }
        };
    }

    @Test
    public void testBlocksDangerousRmCommand() {
        assertFalse("Should block 'rm -rf' command", validator.validate("rm -rf /"));
    }

    @Test
    public void testBlocksSudoCommand() {
        assertFalse("Should block 'sudo' command", validator.validate("sudo apt-get install something"));
    }

    @Test
    public void testAllowsSafeCommand() {
        assertTrue("Should allow 'ls -la' command", validator.validate("ls -la"));
    }

    @Test
    public void testBlocksTooLongCommand() {
        String longCommand = "a".repeat(300);
        assertFalse("Should block command longer than 200 chars", validator.validate(longCommand));
    }

    @Test
    public void testBlocksCommandWithTooManyPipes() {
        String tooManyPipes = "ls | grep test | sort | uniq ";
        assertFalse("Should block command with more than 2 pipes", validator.validate(tooManyPipes));
    }

    @Test
    public void testAllowsSimpleListCommand() {
        assertTrue("Should allow 'ls' command", validator.validate("ls"));
    }
}
