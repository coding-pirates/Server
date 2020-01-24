package de.upb.codingpirates.battleships.server.gui.control.cell;

import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.util.Callback;

import javax.annotation.Nonnull;

/**
 * A {@link TableColumn#cellFactory} implementation that may be used directly inside the {@code .fxml} file.
 *
 * This class removes the need for adding an {@code fx:id} element to each {@code TableColumn} that should contain
 * {@link CheckBoxTableCell}s, adding a field with matching name in the controller and then manually invoking
 * {@link TableColumn#setCellFactory(Callback)} from somewhere in the controller.
 *
 * @param <S> The type of the class contained within the TableView.items list.
 *
 * @author Andre Blanke
 */
public final class CheckBoxTableCellFactory<S> implements Callback<TableColumn<S, Boolean>, TableCell<S, Boolean>> {

    @Override
    public TableCell<S, Boolean> call(@Nonnull final TableColumn<S, Boolean> tableColumn) {
        return new CheckBoxTableCell<>(index -> new ReadOnlyBooleanWrapper(tableColumn.getCellData(index)));
    }
}
