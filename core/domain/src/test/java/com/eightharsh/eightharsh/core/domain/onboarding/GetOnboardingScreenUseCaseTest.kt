package com.eightharsh.eightharsh.core.domain.onboarding

import com.eightharsh.eightharsh.core.domain.repository.AppConfiguration
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class GetOnboardingScreenUseCaseTest {
    private val appConfiguration: AppConfiguration = mockk()
    private val useCase = GetOnboardingScreenUseCase(appConfiguration)

    private val mandatoryScreens = listOf(
        OnboardingScreen.WELCOME,
        OnboardingScreen.STATISTICS,
        OnboardingScreen.FAVORITES,
        OnboardingScreen.DEFAULT_LAUNCHER,
        OnboardingScreen.FINISHED,
    )

    @Test
    fun `API 26 and 27 have only the mandatory setup steps in order`() {
        for (sdk in listOf(26, 27)) {
            every { appConfiguration.sdkInt } returns sdk
            assertEquals(mandatoryScreens, useCase())
        }
    }

    @Test
    fun `API 28 and newer add optional accessibility setup before finishing`() {
        val expected = mandatoryScreens.dropLast(1) + listOf(
            OnboardingScreen.ACCESSIBILITY,
            OnboardingScreen.FINISHED,
        )
        for (sdk in listOf(28, 35, 37)) {
            every { appConfiguration.sdkInt } returns sdk
            assertEquals(expected, useCase())
        }
    }
}
