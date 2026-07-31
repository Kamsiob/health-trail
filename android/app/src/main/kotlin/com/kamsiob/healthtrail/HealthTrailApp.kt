package com.kamsiob.healthtrail

import android.app.Application

/**
 * There is deliberately very little here.
 *
 * No analytics initializer, no crash reporting, no network client, no remote
 * config, and no identity. The app makes no network calls at all, so there is
 * nothing to set up at launch except the app itself.
 */
class HealthTrailApp : Application()
