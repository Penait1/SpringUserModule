package com.erikschouten.usermodule.service

import com.erikschouten.customclasses.exceptions.AlreadyExistsException
import com.erikschouten.customclasses.exceptions.InvalidParameterException
import com.erikschouten.usermodule.AppUserBuilder
import com.erikschouten.usermodule.model.AppUser
import com.erikschouten.usermodule.repository.AppUserRepository
import com.erikschouten.usermodule.service.util.AppUserUtil
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Test
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.*

class AppUserServiceTests {

    private val appUserRepository = mock<AppUserRepository>()
    private val appUserUtil = mock<AppUserUtil>()
    private val passwordEncoder = mock<PasswordEncoder>()
    private val appUserService = AppUserService(appUserRepository, appUserUtil, passwordEncoder)

    @Test
    fun validAppUserCreation() {
        whenever(appUserUtil.get(any<String>())).thenReturn(AppUserBuilder(id = UUID.randomUUID(), email = "TransactionUserTestInvalid@headon.nl").build())
        whenever(passwordEncoder.encode("p")).thenReturn("vErYsEcUrEpAsSwOrD")
        whenever(appUserRepository.save(any<AppUser>()))
                .thenReturn(AppUserBuilder(email = "validAppUserCreation@headon.nl").build())

        appUserService.create("validAppUserCreation@headon.nl", "p")
    }

    @Test(expected = AlreadyExistsException::class)
    fun appUserCreationWithExistingUsername() {
        whenever(passwordEncoder.encode("p")).thenReturn("vErYsEcUrEpAsSwOrD")
        whenever(appUserUtil.emailInUse("appUserCreationWithExistingUsername@headon.nl"))
                .thenReturn(true)

        appUserService.create("appUserCreationWithExistingUsername@headon.nl", "p")
    }

    @Test
    fun appUserUpdate() {
        SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken("appUserUpdate@headon.nl", "p")
        whenever(appUserUtil.emailInUse("appUserUpdate@gmail.com"))
                .thenReturn(false)
        whenever(appUserUtil.findCurrent()).thenReturn(AppUserBuilder(email = "appUserUpdate@headon.nl").build())
        whenever(appUserRepository.save(any<AppUser>())).thenReturn(AppUserBuilder(email = "appUserUpdate@headon.nl").build())

        appUserService.update("appUserUpdate@gmail.com")
    }

    @Test(expected = AlreadyExistsException::class)
    fun appUserUpdateUsernameAlreadyInUse() {
        SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken("appUserUpdate@headon.nl", "p")
        whenever(appUserUtil.get("appUserUpdate@headon.nl"))
                .thenReturn(AppUserBuilder(email = "appUserUpdate@headon.nl").build())
        whenever(appUserUtil.emailInUse("appUserUpdate@gmail.com"))
                .thenReturn(true)
        whenever(appUserUtil.findCurrent()).thenReturn(AppUserBuilder(email = "appUserUpdate@headon.nl").build())

        appUserService.update("appUserUpdate@gmail.com")
    }

    @Test
    fun appUserUpdateAdmin() {
        whenever(appUserUtil.get(UUID.fromString("befa7c20-20ae-42dd-ad1f-b061cce7ad85")))
                .thenReturn(AppUserBuilder(email = "appUserUpdateAdmin@headon.nl").build())
        whenever(appUserRepository.save(any<AppUser>()))
                .thenReturn(AppUserBuilder(email = "appUserUpdateAdmin@headon.nl").build())

        appUserService.update(UUID.fromString("befa7c20-20ae-42dd-ad1f-b061cce7ad85"), "appUserUpdateAdmin@gmail.com", setOf(SimpleGrantedAuthority("ROLE_USERS")), false)
    }

    @Test
    fun appUserUpdateAdminEmailNoChange() {
        whenever(appUserUtil.get(UUID.fromString("befa7c20-20ae-42dd-ad1f-b061cce7ad85")))
                .thenReturn(AppUserBuilder(email = "appUserUpdateAdmin@headon.nl").build())
        whenever(appUserRepository.save(any<AppUser>())).thenReturn(AppUserBuilder(email = "appUserUpdateAdmin@headon.nl").build())

        appUserService.update(UUID.fromString("befa7c20-20ae-42dd-ad1f-b061cce7ad85"), "appUserUpdateAdmin@headon.nl", emptySet(), true)
    }

    @Test(expected = AlreadyExistsException::class)
    fun invalidAppUserUpdate() {
        whenever(appUserUtil.get(UUID.fromString("befa7c20-20ae-42dd-ad1f-b061cce7ad85")))
                .thenReturn(AppUserBuilder(email = "invalidAppUserUpdate@headon.nl").build())

        whenever(appUserUtil.emailInUse("invalidAppUserUpdate@gmail.com"))
                .thenReturn(true)

        appUserService.update(UUID.fromString("befa7c20-20ae-42dd-ad1f-b061cce7ad85"), "invalidAppUserUpdate@gmail.com", setOf(SimpleGrantedAuthority("ROLE_USERS")), false)
    }

    @Test
    fun changePasswordUser() {
        whenever(passwordEncoder.encode("p")).thenReturn("vErYsEcUrEpAsSwOrD")
        whenever(passwordEncoder.encode("q")).thenReturn("DiFfErEnTvErYsEcUrEpAsSwOrD")
        whenever(passwordEncoder.matches("p", "vErYsEcUrEpAsSwOrD")).thenReturn(true)

        val password = passwordEncoder.encode("p")

        whenever(appUserUtil.findCurrent())
                .thenReturn(AppUserBuilder(email = "changePasswordUser@headon.nl", password = password).build())

        SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken("changePasswordUser@headon.nl", "p")
        appUserService.changePassword("p", "q")
    }

    @Test(expected = InvalidParameterException::class)
    fun wrongPasswordChangePasswordUser() {
        whenever(passwordEncoder.encode("p")).thenReturn("vErYsEcUrEpAsSwOrD")
        whenever(passwordEncoder.matches("p", "vErYsEcUrEpAsSwOrD")).thenReturn(true)

        val password = passwordEncoder.encode("p")

        whenever(appUserUtil.findCurrent())
                .thenReturn(AppUserBuilder(email = "wrongPasswordChangePasswordUser@headon.nl", password = password).build())

        SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken("wrongPasswordChangePasswordUser@headon.nl", "p")
        appUserService.changePassword("asd", "q")
    }

    @Test(expected = InvalidParameterException::class)
    fun samePasswordChangePasswordUser() {
        appUserService.changePassword("p", "p")
    }

    @Test
    fun changePasswordAdmin() {
        whenever(passwordEncoder.encode("q")).thenReturn("DiFfErEnTvErYsEcUrEpAsSwOrD")
        whenever(appUserUtil.get(UUID.fromString("befa7c20-20ae-42dd-ad1f-b061cce7ad85")))
                .thenReturn(AppUserBuilder(email = "changePasswordAdmin@headon.nl").build())

        appUserService.changePassword(UUID.fromString("befa7c20-20ae-42dd-ad1f-b061cce7ad85"), "q")
    }

    @Test
    fun listAppUserDTO() {
        whenever(appUserRepository.findAll())
                .thenReturn(listOf(AppUserBuilder().build(), AppUserBuilder(email = "test2@headon.nl").build()))
        appUserService.getAll()
    }

    @Test
    fun getCurrentUser() {
        SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken("getCurrentUser@headon.nl", "p")
        whenever(appUserUtil.findCurrent())
                .thenReturn(AppUserBuilder(email = "getCurrentUser@headon.nl").build())

        val appUser = appUserUtil.findCurrent()
        assert(appUser.email == "getCurrentUser@headon.nl")
    }
}