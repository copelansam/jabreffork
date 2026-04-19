package org.jabref.gui.importer;

import java.util.Optional;

import javafx.collections.FXCollections;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.LibraryTabContainer;
import org.jabref.gui.StateManager;
import org.jabref.gui.newentry.NewEntryDialogTab;
import org.jabref.gui.newentry.NewEntryPreferences;
import org.jabref.gui.newentry.NewEntryView;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.util.OptionalObjectProperty;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NewEntryActionTest {
    private NewEntryAction newEntryAction;

    private final GuiPreferences preferences = mock(GuiPreferences.class);
    private final LibraryTab libraryTab = mock(LibraryTab.class);
    private final LibraryTabContainer tabContainer = mock(LibraryTabContainer.class);
    private final DialogService dialogService = mock(DialogService.class);
    private final StateManager stateManager = mock(StateManager.class);

    @BeforeEach
    void setUp() {
        when(stateManager.activeDatabaseProperty()).thenReturn(OptionalObjectProperty.empty());
        newEntryAction = new NewEntryAction(false, () -> libraryTab, dialogService, preferences, stateManager);
    }

    // NOTE: Test from existing test suite
    @Test
    void executeOnSuccessWithFixedType() {
        EntryType type = StandardEntryType.Article;
        newEntryAction = new NewEntryAction(type, () -> libraryTab, dialogService, preferences, stateManager);
        when(tabContainer.getLibraryTabs()).thenReturn(FXCollections.observableArrayList(libraryTab));

        newEntryAction.execute();
        verify(libraryTab, times(1)).insertEntry(new BibEntry(type));
    }

    // Comment
    @Test
    void executeOnFailureWithNoDatabaseFromShortcut() {

        EntryType type = StandardEntryType.Article;

        // Passing an EntryType object into the constructor will make the isImmediate field be assigned to true
        newEntryAction = new NewEntryAction(type, () -> null, dialogService, preferences, stateManager);

        newEntryAction.execute();
        verify(libraryTab, times(0)).insertEntry(any());
        verify(dialogService, times(0)).showCustomDialogAndWait(any());
    }

    @Test
    void executeOnFailureWithNoDatabaseFromUserDecision() {
        EntryType type = StandardEntryType.Article;
        newEntryAction = new NewEntryAction(false, () -> null, dialogService, preferences, stateManager);

        newEntryAction.execute();
        verify(libraryTab, times(0)).insertEntry(any());
        verify(dialogService, times(0)).showCustomDialogAndWait(any());
    }

    @Test
    public void executeOnSuccessWithNoFixedType() {

        EntryType type = StandardEntryType.Book;

        // Mock preferences and related object
        GuiPreferences preferences = mock(GuiPreferences.class);
        NewEntryPreferences newEntryPrefs = mock(NewEntryPreferences.class);

        // Mock their behaviors, assign return type to return from preferences
        when(preferences.getNewEntryPreferences()).thenReturn(newEntryPrefs);
        when(newEntryPrefs.getLatestImmediateType()).thenReturn(type);

        newEntryAction = new NewEntryAction(true, () -> libraryTab, dialogService, preferences, stateManager);
        newEntryAction.execute();

        // Verify that a BibEntry of type selected at top of test was inserted
        verify(libraryTab, times(1)).insertEntry(new BibEntry(type));
    }

    @Test
    public void executeOnSuccessWithUserSelectedType() {

        BibEntry selectedEntry = new BibEntry(StandardEntryType.Collection);

        // Create a mocked NewEntryView Not doing so will throw an error since UI components have to run on a specific thread unavailable during testing
        // Tells the dialogService to return a new entry of specified type to simulate the user selecting that type
        try (MockedConstruction<NewEntryView> ignored = mockConstruction(NewEntryView.class)) {
            when(dialogService.showCustomDialogAndWait(any(NewEntryView.class)))
                    .thenReturn(Optional.of(selectedEntry));

            newEntryAction = new NewEntryAction((NewEntryDialogTab) null, () -> libraryTab, dialogService, preferences, stateManager);

            newEntryAction.execute();

            verify(libraryTab, times(1)).insertEntry(selectedEntry);
            verify(dialogService, times(1)).showCustomDialogAndWait(any());
        }
    }

    @Test
    public void executeOnFailureWithUserCancellingTypeSelection() {

        // Create a mocked NewEntryView Not doing so will throw an error since UI components have to run on a specific thread unavailable during testing
        try (MockedConstruction<NewEntryView> ignored = mockConstruction(NewEntryView.class)) {
            // Tell the dialogService to return nothing to simulate the user clicking cancel
            when(dialogService.showCustomDialogAndWait(any()))
                    .thenReturn(Optional.empty());

            newEntryAction = new NewEntryAction((NewEntryDialogTab) null, () -> libraryTab, dialogService, preferences, stateManager);

            newEntryAction.execute();

            verify(libraryTab, times(0)).insertEntry(any());
            verify(dialogService, times(1)).showCustomDialogAndWait(any());
        }
    }

    // This test will execute 2 separate newEntryAction execute methods. The first will simulate user selection,
    // and the second will automatically create a new entry of the same type as the first by storing the type of the
    // previous entry and reusing it.
    @Test
    public void executeOnSuccessWhenImmediateCreationAfterDialogCreation() {

        EntryType type = StandardEntryType.Dataset;
        BibEntry selectedEntry = new BibEntry(type);

        // Create a mocked NewEntryView. not doing so will throw an error since UI components have to run on a specific thread unavailable during testing
        // Tells the dialogService to return a new entry of specified type to simulate the user selecting that type
        try (MockedConstruction<NewEntryView> ignored = mockConstruction(NewEntryView.class)) {
            when(dialogService.showCustomDialogAndWait(any(NewEntryView.class)))
                    .thenReturn(Optional.of(selectedEntry));

            newEntryAction = new NewEntryAction((NewEntryDialogTab) null, () -> libraryTab, dialogService, preferences, stateManager);

            newEntryAction.execute();

            verify(libraryTab, times(1)).insertEntry(selectedEntry);
            verify(dialogService, times(1)).showCustomDialogAndWait(any());
        }
        // This marks the end of the first execution. The next execution will immediately create another
        // entry of the same type without asking the user to select a type

        newEntryAction = new NewEntryAction(type, () -> libraryTab, dialogService, preferences, stateManager);

        newEntryAction.execute();

        // Insert entry will have run twice since we ran the execute method twice, but the dialog service will only
        // have been called once since the second execution looked at the previously created type to immediately create another one
        verify(libraryTab,times(2)).insertEntry(new BibEntry(type));
        verify(dialogService,times(1)).showCustomDialogAndWait(any());
    }
}
