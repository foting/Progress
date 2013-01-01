package se.uu.it.android.progress;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.NumberPicker;

public class CustomSetDialog extends AlertDialog implements OnClickListener {
    private OnNumberSetListener mListener;
    private NumberPicker mActivePicker;
    private NumberPicker mPassivePicker;
    private NumberPicker mRepetitionPicker;

    private int mInitialActiveValue;
    private int mInitialPassiveValue;
    private int mInitialRepetitionValue;

    public CustomSetDialog(Context context, int theme, int initialActiveValue, int initialPassiveValue, int initialRepetitionValue) {
        super(context, theme);
        mInitialActiveValue = initialActiveValue;
        mInitialPassiveValue = initialPassiveValue;
        mInitialRepetitionValue = initialRepetitionValue;

        setButton(BUTTON_POSITIVE, context.getString(R.string.dialog_set_number), this);
        setButton(BUTTON_NEGATIVE, context.getString(R.string.dialog_cancel), (OnClickListener) null);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.dialog_add_set, null);
        setView(view);

        mActivePicker = (NumberPicker) view.findViewById(R.id.active_picker);
        mActivePicker.setMinValue(1);
        mActivePicker.setMaxValue(300);
        mActivePicker.setValue(mInitialActiveValue);
        
        mPassivePicker = (NumberPicker) view.findViewById(R.id.passive_picker);
        mPassivePicker.setMinValue(0);
        mPassivePicker.setMaxValue(300);
        mPassivePicker.setValue(mInitialPassiveValue);
        
        mRepetitionPicker = (NumberPicker) view.findViewById(R.id.repetition_picker);
        mRepetitionPicker.setMinValue(1);
        mRepetitionPicker.setMaxValue(300);
        mRepetitionPicker.setValue(mInitialRepetitionValue);
    }

    public void setOnNumberSetListener(OnNumberSetListener listener) {
        mListener = listener;
    }

    @Override
	public void onClick(DialogInterface dialog, int which) {
        if (mListener != null) {
            mListener.onNumberSet(mActivePicker.getValue());
            mListener.onNumberSet(mPassivePicker.getValue());
            mListener.onNumberSet(mRepetitionPicker.getValue());
        }
    }

    public interface OnNumberSetListener {
        public void onNumberSet(int selectedNumber);
    }
}
