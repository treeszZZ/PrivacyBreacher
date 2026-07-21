package io.nandandesai.privacybreacher;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ConfirmAdapter extends RecyclerView.Adapter<ConfirmAdapter.ViewHolder> {

    private List<ConfirmRecord> records = new ArrayList<>();
    private Context context;
    private OnConfirmListener listener;

    public interface OnConfirmListener {
        void onConfirm(long id);
    }

    public ConfirmAdapter(Context context, OnConfirmListener listener) {
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_confirm_record, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ConfirmRecord record = records.get(position);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日 E", Locale.CHINA);
        String displayDate = sdf.format(new Date(record.getTimestamp()));
        holder.tvDate.setText(displayDate);
        holder.tvTime.setText(record.getTime());
        holder.btnConfirm.setText("确认");
        holder.btnConfirm.setOnClickListener(v -> {
            if (listener != null) {
                listener.onConfirm(record.getId());
            }
        });
    }

    @Override
    public int getItemCount() {
        return records.size();
    }

    public void setRecords(List<ConfirmRecord> records) {
        this.records = records;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvTime;
        Button btnConfirm;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.dateText);
            tvTime = itemView.findViewById(R.id.timeText);
            btnConfirm = itemView.findViewById(R.id.confirmButton);
        }
    }
}
