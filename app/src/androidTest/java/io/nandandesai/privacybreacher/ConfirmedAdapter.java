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

public class ConfirmedAdapter extends RecyclerView.Adapter<ConfirmedAdapter.ViewHolder> {

    private List<ConfirmedRecord> records = new ArrayList<>();
    private Context context;
    private HomeFragment parent;

    public ConfirmedAdapter(Context context, HomeFragment parent) {
        this.context = context;
        this.parent = parent;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_confirmed_record, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ConfirmedRecord record = records.get(position);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日 E", Locale.CHINA);
        String displayDate = sdf.format(new Date(record.getTimestamp()));
        holder.tvDate.setText(displayDate);
        holder.tvTime.setText(record.getTime());
        holder.btnWithdraw.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(context)
                    .setTitle("撤回确认")
                    .setMessage("确认撤回该记录？")
                    .setPositiveButton("确认", (d, which) -> {
                        if (parent != null) {
                            parent.onWithdraw(record.getId());
                        }
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });
    }

    @Override
    public int getItemCount() {
        return records.size();
    }

    public void setRecords(List<ConfirmedRecord> records) {
        this.records = records;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvTime;
        Button btnWithdraw;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvConfirmedDate);
            tvTime = itemView.findViewById(R.id.tvConfirmedTime);
            btnWithdraw = itemView.findViewById(R.id.btnWithdraw);
        }
    }
}
