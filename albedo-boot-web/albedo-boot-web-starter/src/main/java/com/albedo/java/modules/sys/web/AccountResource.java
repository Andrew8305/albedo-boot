package com.albedo.java.modules.sys.web;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.albedo.java.common.security.SecurityUtil;
import com.albedo.java.modules.sys.domain.PersistentToken;
import com.albedo.java.modules.sys.domain.User;
import com.albedo.java.modules.sys.repository.PersistentTokenRepository;
import com.albedo.java.modules.sys.repository.UserRepository;
import com.albedo.java.modules.sys.service.UserService;
import com.albedo.java.util.PublicUtil;
import com.albedo.java.util.base.Assert;
import com.albedo.java.util.exception.RuntimeMsgException;
import com.albedo.java.web.rest.ResultBuilder;
import com.albedo.java.web.rest.base.BaseResource;
import com.codahale.metrics.annotation.Timed;

//import com.albedo.java.web.rest.vm.KeyAndPasswordVM;
//import com.albedo.java.web.rest.vm.ManagedUserVM;

//import com.albedo.java.modules.sys.service.MailService;

/**
 * REST controller for managing the current user's account.
 */
@Controller
@RequestMapping("${albedo.adminPath}/api")
public class AccountResource extends BaseResource {

    private final Logger log = LoggerFactory.getLogger(AccountResource.class);
    @Autowired(required = false)
    private PasswordEncoder passwordEncoder;
    @Resource
    private UserRepository userRepository;

    @Resource
    private UserService userService;

    @Resource
    private PersistentTokenRepository persistentTokenRepository;

//    @Resource
//    private MailService mailService;

    /**
     * POST  /register : register the user.
     *
     * @param managedUserVM the managed user View Model
     * @param request the HTTP request
     * @return the ResponseEntity with status 201 (Created) if the user is registered or 400 (Bad Request) if the login or e-mail is already in use
     */
//    @RequestMapping(value = "/register",
//                    method = RequestMethod.POST,
//                    produces={MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_PLAIN_VALUE})
//    @Timed
//    public ResponseEntity<?> registerAccount(@Valid @RequestBody ManagedUserVM managedUserVM, HttpServletRequest request) {
//
//        HttpHeaders textPlainHeaders = new HttpHeaders();
//        textPlainHeaders.setContentType(MediaType.TEXT_PLAIN);
//
//        return userRepository.findOneByLoginId(managedUserVM.getLoginId().toLowerCase())
//            .map(user -> new ResponseEntity<>("login already in use", textPlainHeaders, HttpStatus.BAD_REQUEST))
//            .orElseGet(() -> userRepository.findOneByEmail(managedUserVM.getEmail())
//                .map(user -> new ResponseEntity<>("e-mail address already in use", textPlainHeaders, HttpStatus.BAD_REQUEST))
//                .orElseGet(() -> {
//                    User user = userService.create(managedUserVM.getLoginId(), managedUserVM.getPassword(),
//                    managedUserVM.getName(), managedUserVM.getEmail().toLowerCase(),
//                    managedUserVM.getLangKey());
//                    String baseUrl = request.getScheme() + // "http"
//                    "://" +                                // "://"
//                    request.getServerName() +              // "myhost"
//                    ":" +                                  // ":"
//                    request.getServerPort() +              // "80"
//                    request.getContextPath();              // "/myContextPath" or "" if deployed in root context
//
//                    mailService.sendActivationEmail(user, baseUrl);
//                    return new ResponseEntity<>(HttpStatus.CREATED);
//                })
//        );
//    }

    /**
     * GET  /activate : activate the registered user.
     *
     * @param key the activation key
     * @return the ResponseEntity with status 200 (OK) and the activated user in body, or status 500 (Internal Server Error) if the user couldn't be activated
     */
    @RequestMapping(value = "/activate",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<String> activateAccount(@RequestParam(value = "key") String key) {
        return userService.activateRegistration(key)
                .map(user -> new ResponseEntity<String>(HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));
    }

    /**
     * GET  /authenticate : check if the user is authenticated, and return its login.
     *
     * @param request the HTTP request
     * @return the login if the user is authenticated
     */
    @RequestMapping(value = "/authenticate",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public String isAuthenticated(HttpServletRequest request) {
        log.debug("REST request to check if the current user is authenticated");
        return request.getRemoteUser();
    }

//    /**
//     * GET  /account : get the current user.
//     *
//     * @return the ResponseEntity with status 200 (OK) and the current user in body, or status 500 (Internal Server Error) if the user couldn't be returned
//     */
//    @RequestMapping(value = "/account",
//        method = RequestMethod.GET,
//        produces = MediaType.APPLICATION_JSON_VALUE)
//    @Timed
//    public ResponseEntity<UserDTO> getAccount() {
//        return Optional.ofNullable(userService.getUserWithAuthorities())
//            .map(user -> new ResponseEntity<>(new UserDTO(user), HttpStatus.OK))
//            .orElse(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));
//    }
//
//    /**
//     * POST  /account : update the current user information.
//     *
//     * @param userDTO the current user information
//     * @return the ResponseEntity with status 200 (OK), or status 400 (Bad Request) or 500 (Internal Server Error) if the user couldn't be updated
//     */
//    @RequestMapping(value = "/account",
//        method = RequestMethod.POST,
//        produces = MediaType.APPLICATION_JSON_VALUE)
//    @Timed
//    public ResponseEntity<String> saveAccount(@Valid @RequestBody UserDTO userDTO) {
//        Optional<User> existingUser = userRepository.findOneByEmail(userDTO.getEmail());
//        if (existingUser.isPresent() && (!existingUser.get().getLoginId().equalsIgnoreCase(userDTO.getLoginId()))) {
//            return ResponseEntity.badRequest().headers(HeaderUtil.createFailureAlert("user-management", "emailexists", "Email already in use")).body(null);
//        }
//        return userRepository
//        		.findOneById(SecurityUtil.getCurrentUserId())
//            .map(u -> {
//                userService.update(userDTO.getName(), userDTO.getEmail(),
//                    userDTO.getLangKey());
//                return new ResponseEntity<String>(HttpStatus.OK);
//            })
//            .orElseGet(() -> new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));
//    }

    /**
     * @return
     */
    @RequestMapping(value = "/account/changePassword",
            method = RequestMethod.GET,
            produces = MediaType.TEXT_PLAIN_VALUE)
    @Timed
    public String changePassword() {
        return "modules/sys/changePassword";
    }

    /**
     * @param password
     * @param newPassword
     * @param confirmPassword
     * @return
     */
    @RequestMapping(value = "/account/changePassword",
            method = RequestMethod.POST,
            produces = MediaType.TEXT_PLAIN_VALUE)
    @Timed
    public ResponseEntity changePassword(String password, String newPassword, String confirmPassword) {

        if (PublicUtil.isEmpty(password) || PublicUtil.isEmpty(newPassword) || PublicUtil.isEmpty(confirmPassword)) {
            throw new RuntimeMsgException("新旧密码不能为空");
        }
        if (password.equals(newPassword)) {
            throw new RuntimeMsgException("新旧密码不能一致");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new RuntimeMsgException("新密码与确认密码不一致");
        }
        User user = userService.findOne(SecurityUtil.getCurrentUserId());
        Assert.assertNotNull(user, "无法获取用户信息");
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeMsgException("旧密码输入有误");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userService.save(user);
        log.debug("Changed password for User: {}", user);
        return ResultBuilder.buildOk("密码修改成功，请下次登录时使用新密码");
    }

    /**
     * GET  /account/sessions : get the current open sessions.
     *
     * @return the ResponseEntity with status 200 (OK) and the current open sessions in body,
     * or status 500 (Internal Server Error) if the current open sessions couldn't be retrieved
     */
    @RequestMapping(value = "/account/sessions",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<List<PersistentToken>> getCurrentSessions() {
        User user = userRepository.findOneById(SecurityUtil.getCurrentUserId());
        return new ResponseEntity(
                persistentTokenRepository.findAllByUserId(user.getId()),
                HttpStatus.OK);
    }

    /**
     * DELETE  /account/sessions?series={series} : invalidate an existing session.
     * <p>
     * - You can only delete your own sessions, not any other user's session
     * - If you delete one of your existing sessions, and that you are currently logged in on that session, you will
     * still be able to use that session, until you quit your browser: it does not work in real time (there is
     * no API for that), it only removes the "remember me" cookie
     * - This is also true if you invalidate your current session: you will still be able to use it until you close
     * your browser or that the session times out. But automatic login (the "remember me" cookie) will not work
     * anymore.
     * There is an API to invalidate the current session, but there is no API to check which session uses which
     * cookie.
     *
     * @param series the series of an existing session
     * @throws UnsupportedEncodingException if the series couldnt be URL decoded
     */
    @RequestMapping(value = "/account/sessions/{series}",
            method = RequestMethod.DELETE)
    @Timed
    public void invalidateSession(@PathVariable String series) throws UnsupportedEncodingException {
        String decodedSeries = URLDecoder.decode(series, "UTF-8");
        User user = userRepository.findOneById(SecurityUtil.getCurrentUserId());
        persistentTokenRepository.findAllByUserId(user.getId()).stream()
                .filter(persistentToken -> StringUtils.equals(persistentToken.getSeries(), decodedSeries))
                .findAny().ifPresent(t -> persistentTokenRepository.delete(t));
    }

    /**
     * POST   /account/reset_password/init : Send an e-mail to reset the password of the user
     *
     * @param mail the mail of the user
     * @param request the HTTP request
     * @return the ResponseEntity with status 200 (OK) if the e-mail was sent, or status 400 (Bad Request) if the e-mail address is not registered
     */
//    @RequestMapping(value = "/account/reset_password/init",
//        method = RequestMethod.POST,
//        produces = MediaType.TEXT_PLAIN_VALUE)
//    @Timed
//    public ResponseEntity<?> requestPasswordReset(@RequestBody String mail, HttpServletRequest request) {
//        return userService.requestPasswordReset(mail)
//            .map(user -> {
//                String baseUrl = request.getScheme() +
//                    "://" +
//                    request.getServerName() +
//                    ":" +
//                    request.getServerPort() +
//                    request.getContextPath();
//                mailService.sendPasswordResetMail(user, baseUrl);
//                return new ResponseEntity<>("e-mail was sent", HttpStatus.OK);
//            }).orElse(new ResponseEntity<>("e-mail address not registered", HttpStatus.BAD_REQUEST));
//    }

//    /**
//     * POST   /account/reset_password/finish : Finish to reset the password of the user
//     *
//     * @param keyAndPassword the generated key and the new password
//     * @return the ResponseEntity with status 200 (OK) if the password has been reset,
//     * or status 400 (Bad Request) or 500 (Internal Server Error) if the password could not be reset
//     */
//    @RequestMapping(value = "/account/reset_password/finish",
//        method = RequestMethod.POST,
//        produces = MediaType.TEXT_PLAIN_VALUE)
//    @Timed
//    public ResponseEntity<String> finishPasswordReset(@RequestBody KeyAndPasswordVM keyAndPassword) {
//        if (!checkPasswordLength(keyAndPassword.getNewPassword())) {
//            return new ResponseEntity<>("Incorrect password", HttpStatus.BAD_REQUEST);
//        }
//        return userService.completePasswordReset(keyAndPassword.getNewPassword(), keyAndPassword.getKey())
//              .map(user -> new ResponseEntity<String>(HttpStatus.OK))
//              .orElse(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));
//    }
//
//    private boolean checkPasswordLength(String password) {
//        return (!StringUtils.isEmpty(password) &&
//            password.length() >= ManagedUserVM.PASSWORD_MIN_LENGTH &&
//            password.length() <= ManagedUserVM.PASSWORD_MAX_LENGTH);
//    }
}
