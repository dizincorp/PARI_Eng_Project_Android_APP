package com.example.project.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.project.databinding.ItemVehicleBinding;
import com.example.project.model.Vehicle;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for displaying and deleting user vehicles in the profile dialog.
 */
public class VehiclesAdapter extends RecyclerView.Adapter<VehiclesAdapter.VehicleViewHolder> {

    /**
     * Callback interface for vehicle deletion events.
     */
    public interface OnVehicleDeleteClickListener {
        /**
         * Invoked when user requests vehicle deletion.
         *
         * @param vehicle Selected vehicle to delete
         */
        void onVehicleDelete(Vehicle vehicle);
    }

    private final List<Vehicle> vehicles = new ArrayList<>();
    private final OnVehicleDeleteClickListener deleteListener;

    /**
     * Constructor for VehiclesAdapter.
     *
     * @param deleteListener Callback listener for delete button clicks
     */
    public VehiclesAdapter(OnVehicleDeleteClickListener deleteListener) {
        this.deleteListener = deleteListener;
    }

    /**
     * Updates the adapter list with new vehicle items.
     *
     * @param newVehicles New list of vehicles
     */
    public void submitList(List<Vehicle> newVehicles) {
        this.vehicles.clear();
        if (newVehicles != null) {
            this.vehicles.addAll(newVehicles);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VehicleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemVehicleBinding binding = ItemVehicleBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new VehicleViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull VehicleViewHolder holder, int position) {
        Vehicle vehicle = vehicles.get(position);
        holder.bind(vehicle, deleteListener);
    }

    @Override
    public int getItemCount() {
        return vehicles.size();
    }

    /**
     * ViewHolder caching View Binding for vehicle item row.
     */
    static class VehicleViewHolder extends RecyclerView.ViewHolder {

        private final ItemVehicleBinding binding;

        VehicleViewHolder(@NonNull ItemVehicleBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Vehicle vehicle, OnVehicleDeleteClickListener deleteListener) {
            if (vehicle == null) return;

            binding.textVehicleModel.setText(vehicle.getModel());
            binding.textVehiclePlate.setText(vehicle.getPlateNumber());

            binding.btnDeleteVehicle.setOnClickListener(v -> {
                if (deleteListener != null) {
                    deleteListener.onVehicleDelete(vehicle);
                }
            });
        }
    }
}
