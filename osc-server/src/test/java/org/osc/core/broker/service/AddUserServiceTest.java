package org.osc.core.broker.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.hibernate.Session;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osc.core.broker.model.entities.User;
import org.osc.core.broker.service.AddUserService;
import org.osc.core.broker.service.dto.UserDtoValidator;
import org.osc.core.broker.service.exceptions.VmidcBrokerInvalidEntryException;
import org.osc.core.broker.service.request.AddUserRequest;
import org.osc.core.broker.service.response.AddUserResponse;
import org.osc.core.broker.util.SessionStub;

public class AddUserServiceTest {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Mock
    private Session sessionMock;

    @Mock
    private UserDtoValidator validatorMock;

    @InjectMocks
    private AddUserService service;

    private AddUserRequest invalidUserRequest;

    private SessionStub sessionStub;

    @Before
    public void testInitialize() throws Exception{
        MockitoAnnotations.initMocks(this);
        this.sessionStub = new SessionStub(this.sessionMock);

        this.invalidUserRequest = new AddUserRequest();
        this.invalidUserRequest.setLoginName("invalidUserName");

        Mockito.doThrow(VmidcBrokerInvalidEntryException.class).when(this.validatorMock).validateForCreate(this.invalidUserRequest);
    }

    @Test
    public void testDispatch_WithNullRequest_ThrowsNullPointerException() throws Exception{
        // Arrange.
        this.exception.expect(NullPointerException.class);

        // Act.
        this.service.dispatch(null);
    }

    @Test
    public void testDispatch_WhenUserValidationFails_ThrowsInvalidEntryException() throws Exception{
        // Arrange.
        this.exception.expect(VmidcBrokerInvalidEntryException.class);

        // Act.
        this.service.dispatch(this.invalidUserRequest);

        // Assert.
        Mockito.verify(this.validatorMock).validateForCreate(this.invalidUserRequest);
    }

    @Test
    public void testDispatch_AddingNewUser_ExpectsValidResponse() throws Exception{
        // Arrange.
        AddUserRequest request = new AddUserRequest();
        request.setLoginName("userName");
        Long userId = 45L;
        this.sessionStub.stubSaveEntity(new UserLoginMatcher(request.getLoginName()), userId);

        // Act.
        AddUserResponse response = this.service.dispatch(request);

        // Assert.
        Mockito.verify(this.validatorMock).validateForCreate(request);
        assertNotNull("The response of add user should not be null.", response);
        assertEquals("The returned id was different than expected.", userId.longValue(), response.getId());
    }

    private class UserLoginMatcher extends ArgumentMatcher<Object> {
        private String loginName;

        UserLoginMatcher(String loginName) {
            this.loginName = loginName;
        }

        @Override
        public boolean matches(Object object) {
            if (object == null || !(object instanceof User)) {
                return false;
            }

            User providedUser = (User) object;

            return this.loginName.equals(providedUser.getLoginName());
        }
    }
}
