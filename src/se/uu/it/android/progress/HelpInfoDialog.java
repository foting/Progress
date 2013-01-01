package se.uu.it.android.progress;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.view.LayoutInflater;
import android.view.View;

public class HelpInfoDialog extends AlertDialog implements OnClickListener {

    public HelpInfoDialog(Context context, int theme) {
        super(context, theme);
        
        setButton(BUTTON_NEGATIVE, context.getString(R.string.dialog_close), (OnClickListener) null);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.dialog_help_info, null);
        setView(view);

    }

    @Override
	public void onClick(DialogInterface dialog, int which) {
        
    }
}
