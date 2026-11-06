package com.eightharsh.eightharsh.core.domain.challenges

import com.eightharsh.eightharsh.core.domain.repository.android.AppsRepository
import com.eightharsh.eightharsh.core.domain.repository.db.ModifiedAppsRepository
import com.eightharsh.eightharsh.core.model.InstalledApp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class GetOpenChallengeAppsUseCase @Inject constructor(
    private val appsRepository: AppsRepository,
    private val modifiedAppsRepository: ModifiedAppsRepository
)
{
    operator fun invoke(): Flow<List<InstalledApp>> {
        return combine(
            appsRepository.mainUserApps,
            modifiedAppsRepository.getChallengePackageIdsFlow()
        ) { apps, challengeIds ->
            val idSet = challengeIds.toSet()
            apps.filter { it.packageName in idSet }
        }
    }
}