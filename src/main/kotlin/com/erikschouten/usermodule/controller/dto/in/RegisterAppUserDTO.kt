package com.erikschouten.usermodule.controller.dto.`in`

import com.erikschouten.customclasses.validators.Password
import javax.validation.constraints.Email

data class RegisterAppUserDTO(@field:[Email] val email: String,
                              @field:[Password] val password: String)
