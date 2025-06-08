
package com.example.demo

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class ApiHelloController(
    private val userService: UserService,
    private val adminService: AdminService
) {

    @GetMapping("/hello")
    fun hello(): String {
        val authentication = SecurityContextHolder.getContext().authentication
        return """
                Hello ${authentication.name} from protected resource!
                Your roles: ${authentication.authorities.joinToString()} 
            """.trimIndent()
    }

    @GetMapping("/user/data")
    fun userData(): String = userService.getData()

    @GetMapping("/admin/data")
    fun adminData(): String = adminService.getData()
}

@Service
class UserService {

    @PreAuthorize("hasRole('read')")
    fun getData(): String {
        return "Detta är skyddad data endast för användare med rollen user."
    }
}

@Service
@PreAuthorize("hasRole('write')")
class AdminService {

    fun getData(): String {
        val authentication = SecurityContextHolder.getContext().authentication
        println("authentication = $authentication")
        return "Detta är skyddad data endast för användare med rollen admin."
    }
}
