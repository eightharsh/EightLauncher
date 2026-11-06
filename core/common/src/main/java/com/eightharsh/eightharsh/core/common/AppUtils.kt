package com.eightharsh.eightharsh.core.common

import android.os.Process
import com.eightharsh.eightharsh.core.model.InstalledApp

/**
 * Checks if the app belongs to the main user.
 */
fun InstalledApp.isMainUserApp(): Boolean = this.user == Process.myUserHandle()
