package com.example

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MainActivityRobolectricTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun testMainActivityLaunch() {
    val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.AppDatabase::class.java).build()
    db.customerDao() // verify creation
    composeTestRule.onNodeWithText("SINGH MILK DAIRY").assertExists()
  }
}
