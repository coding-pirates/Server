package de.upb.codingpirates.battleships.server.gui.control.cell;

import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.util.Callback;

import javax.annotation.Nonnull;

public final class CheckBoxTableCellFactory<S> implements Callback<TableColumn<S, Boolean>, TableCell<S, Boolean>> {

    @Override
    public TableCell<S, Boolean> call(@Nonnull final TableColumn<S, Boolean> tableColumn) {
        return new CheckBoxTableCell<>(i -> new ReadOnlyBooleanWrapper(tableColumn.getCellData(i)));
    }
}
