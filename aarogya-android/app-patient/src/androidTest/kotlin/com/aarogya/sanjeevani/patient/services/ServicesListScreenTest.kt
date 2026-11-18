package com.aarogya.sanjeevani.patient.services

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aarogya.sanjeevani.core.data.model.ServiceMode
import com.aarogya.sanjeevani.core.data.services.Service
import com.aarogya.sanjeevani.core.data.services.ServiceCategory
import com.aarogya.sanjeevani.core.designsystem.theme.AarogyaTheme
import kotlinx.datetime.Instant
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private val SOME_INSTANT = Instant.parse("2026-01-01T00:00:00Z")

/**
 * Exercises the stateless [ServicesListScreen] directly -- same pattern
 * as `HomeScreenTest.kt` -- with a small test-local reducer standing in
 * for [ServicesListViewModel]'s own `onAction` handling, so typing in
 * the search field and tapping a category chip actually re-render the
 * list through the real [groupServicesForDisplay], without needing a
 * ViewModel, Hilt, or a real debounce delay in this test (the debounce
 * itself is [ServicesListViewModelTest]'s job, with precise control over
 * virtual time -- this test only needs to verify what the *result* of
 * filtering looks like on screen).
 */
@RunWith(AndroidJUnit4::class)
class ServicesListScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val generalCategory = aCategory(id = "cat-general", name = "General", sortOrder = 0)
    private val labCategory = aCategory(id = "cat-lab", name = "Lab Tests", sortOrder = 1)
    private val checkup = aService(id = "svc-checkup", categoryId = "cat-general", name = "General Checkup")
    private val bloodTest = aService(id = "svc-blood", categoryId = "cat-lab", name = "Blood Test")

    private val section =
        ServicesListSectionState.Success(
            categories = listOf(generalCategory, labCategory),
            services = listOf(checkup, bloodTest),
        )

    @Test
    fun typingInSearch_filtersTheListToMatchingServicesOnly() {
        setContentWithReducer(initialState())

        composeTestRule.onNodeWithText("General Checkup").assertExists()
        composeTestRule.onNodeWithText("Blood Test").assertExists()

        composeTestRule.onNodeWithText("Search services").performTextInput("blood")

        composeTestRule.onNodeWithText("Blood Test").assertExists()
        composeTestRule.onNodeWithText("General Checkup").assertDoesNotExist()
    }

    @Test
    fun selectingACategoryChip_filtersToJustThatCategory() {
        setContentWithReducer(initialState())

        // "Lab Tests" also appears, non-clickably, as the section header
        // above its grouped services once that category's services are
        // showing, so this matches on hasClickAction() too, to find the
        // filter chip specifically.
        composeTestRule.onNode(hasText("Lab Tests").and(hasClickAction())).performClick()

        composeTestRule.onNodeWithText("Blood Test").assertExists()
        composeTestRule.onNodeWithText("General Checkup").assertDoesNotExist()
    }

    @Test
    fun tappingASelectedCategoryChipAgain_clearsBackToAll() {
        setContentWithReducer(initialState())

        composeTestRule.onNode(hasText("Lab Tests").and(hasClickAction())).performClick()
        composeTestRule.onNodeWithText("General Checkup").assertDoesNotExist()

        composeTestRule.onNode(hasText("Lab Tests").and(hasClickAction())).performClick()

        composeTestRule.onNodeWithText("General Checkup").assertExists()
        composeTestRule.onNodeWithText("Blood Test").assertExists()
    }

    @Test
    fun searchAndCategoryFilterTogether_canYieldNoResults_andClearFiltersRestoresEverything() {
        setContentWithReducer(initialState())

        composeTestRule.onNode(hasText("Lab Tests").and(hasClickAction())).performClick()
        composeTestRule.onNodeWithText("Search services").performTextInput("checkup")

        composeTestRule.onNodeWithText("No services match your search.").assertExists()

        composeTestRule.onNodeWithText("Clear filters").performClick()

        composeTestRule.onNodeWithText("General Checkup").assertExists()
        composeTestRule.onNodeWithText("Blood Test").assertExists()
    }

    private fun initialState() =
        ServicesListUiState(
            section = section,
            groups = groupServicesForDisplay(section.categories, section.services, null, ""),
        )

    private fun setContentWithReducer(initial: ServicesListUiState) {
        composeTestRule.setContent {
            var state by mutableStateOf(initial)
            AarogyaTheme {
                ServicesListScreen(
                    uiState = state,
                    onAction = { action -> state = reduce(state, action) },
                    onServiceClick = {},
                )
            }
        }
    }

    private fun reduce(
        state: ServicesListUiState,
        action: ServicesListAction,
    ): ServicesListUiState {
        val success = state.section as ServicesListSectionState.Success
        return when (action) {
            is ServicesListAction.SearchQueryChanged ->
                state.copy(searchQuery = action.query)
                    .let {
                        it.copy(
                            groups = groupServicesForDisplay(success.categories, success.services, it.selectedCategoryId, it.searchQuery),
                        )
                    }
            is ServicesListAction.CategorySelected ->
                state.copy(selectedCategoryId = action.categoryId)
                    .let {
                        it.copy(
                            groups = groupServicesForDisplay(success.categories, success.services, it.selectedCategoryId, it.searchQuery),
                        )
                    }
            ServicesListAction.ClearFilters ->
                state.copy(searchQuery = "", selectedCategoryId = null)
                    .let { it.copy(groups = groupServicesForDisplay(success.categories, success.services, null, "")) }
            else -> state
        }
    }

    private fun aCategory(
        id: String,
        name: String,
        sortOrder: Int,
    ) = ServiceCategory(
        id = id,
        name = name,
        slug = name.lowercase(),
        description = null,
        icon = null,
        isActive = true,
        sortOrder = sortOrder,
        createdAt = SOME_INSTANT,
        updatedAt = SOME_INSTANT,
    )

    private fun aService(
        id: String,
        categoryId: String,
        name: String,
    ) = Service(
        id = id,
        categoryId = categoryId,
        name = name,
        slug = name.lowercase(),
        description = null,
        shortDescription = "Short description",
        requiresDoctor = false,
        durationMinutes = 30,
        supportedModes = listOf(ServiceMode.IN_PERSON),
        metadata = null,
        isActive = true,
        sortOrder = 0,
        createdAt = SOME_INSTANT,
        updatedAt = SOME_INSTANT,
    )
}
