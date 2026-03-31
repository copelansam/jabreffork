package org.jabref.gui.importer;

import javafx.collections.FXCollections;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.LibraryTabContainer;
import org.jabref.gui.StateManager;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.util.OptionalObjectProperty;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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

    @Test
    void executeOnSuccessWithFixedType() {
        EntryType type = StandardEntryType.Article;
        newEntryAction = new NewEntryAction(type, () -> libraryTab, dialogService, preferences, stateManager);
        when(tabContainer.getLibraryTabs()).thenReturn(FXCollections.observableArrayList(libraryTab));

        newEntryAction.execute();
        verify(libraryTab, times(1)).insertEntry(new BibEntry(type));
    }

    // Passing an EntryType object into the constructor will make the isImmediate field be assigned to true
    @Test
    void executeOnFailureWithNoDatabaseFromShortcut(){
        EntryType type = StandardEntryType.Article;
        newEntryAction = new NewEntryAction(type, () -> null, dialogService, preferences, stateManager);

        newEntryAction.execute();
        verify(libraryTab, times(0)).insertEntry(any());
        verify(dialogService, times(0)).showCustomDialogAndWait(any());
    }

    @Test
    void executeOnFailureWithNoDatabaseFromUserDecision(){
        EntryType type = StandardEntryType.Article;
        newEntryAction = new NewEntryAction(false, () -> null, dialogService, preferences, stateManager);

        newEntryAction.execute();
        verify(libraryTab, times(0)).insertEntry(any());
        verify(dialogService, times(0)).showCustomDialogAndWait(any());
    }
}
