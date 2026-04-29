package com.winlator.cmod;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.winlator.cmod.contentdialog.ContentDialog;
import com.winlator.cmod.contents.AdrenotoolsManager;
import com.winlator.cmod.contents.Downloader;
import com.winlator.cmod.core.AppUtils;
import com.winlator.cmod.core.PreloaderDialog;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AdrenotoolsFragment extends Fragment {
    private static final String GITHUB_RELEASES_API = "https://api.github.com/repos/K11MCH1/AdrenoToolsDrivers/releases";

    private AdrenotoolsManager adrenotoolsManager;
    private RecyclerView recyclerView;
    private RecyclerView remoteRecyclerView;
    private RemoteDriversAdapter remoteDriversAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.adrenotoolsManager = new AdrenotoolsManager(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup layout = (ViewGroup) inflater.inflate(R.layout.adrenotools_fragment, container, false);

        recyclerView = layout.findViewById(R.id.RecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(recyclerView.getContext()));
        recyclerView.addItemDecoration(new DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL));
        recyclerView.setAdapter(new DriversAdapter(adrenotoolsManager.enumarateInstalledDrivers()));

        remoteRecyclerView = layout.findViewById(R.id.RVRemoteDrivers);
        remoteRecyclerView.setLayoutManager(new LinearLayoutManager(remoteRecyclerView.getContext()));
        remoteRecyclerView.addItemDecoration(new DividerItemDecoration(remoteRecyclerView.getContext(), DividerItemDecoration.VERTICAL));
        remoteDriversAdapter = new RemoteDriversAdapter(new ArrayList<>());
        remoteRecyclerView.setAdapter(remoteDriversAdapter);

        View btInstallDriver = layout.findViewById(R.id.BTInstallDriver);
        btInstallDriver.setOnClickListener(v -> ContentDialog.confirm(
                getContext(),
                getString(R.string.install_drivers_message) + " " + getString(R.string.install_drivers_warning),
                this::openLocalDriverPicker
        ));

        loadRemoteDrivers();
        return layout;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(R.string.adrenotools_gpu_drivers);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == MainActivity.OPEN_FILE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Uri uri = data != null ? data.getData() : null;
            installDriverUri(uri);
        }
    }

    private void openLocalDriverPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        getActivity().startActivityFromFragment(this, intent, MainActivity.OPEN_FILE_REQUEST_CODE);
    }

    private void installDriverUri(Uri uri) {
        if (uri == null) {
            AppUtils.showToast(getContext(), R.string.unable_to_import_profile);
            return;
        }

        String driver = adrenotoolsManager.installDriver(uri);
        if (!driver.isEmpty()) {
            ((DriversAdapter) recyclerView.getAdapter()).addItem(driver);
        } else {
            AppUtils.showToast(getContext(), R.string.unable_to_import_profile);
        }
    }

    private void loadRemoteDrivers() {
        Activity activity = getActivity();
        if (activity == null) return;

        PreloaderDialog preloaderDialog = new PreloaderDialog(activity);
        preloaderDialog.showOnUiThread(R.string.downloading_file);

        new Thread(() -> {
            List<RemoteDriverAsset> assets = fetchRemoteDriverAssets();
            activity.runOnUiThread(() -> {
                preloaderDialog.closeOnUiThread();
                remoteDriversAdapter.setItems(assets);
            });
        }).start();
    }

    private void downloadAndInstallRemoteDriver(RemoteDriverAsset asset) {
        Activity activity = getActivity();
        if (activity == null) return;

        PreloaderDialog preloaderDialog = new PreloaderDialog(activity);
        preloaderDialog.showOnUiThread(R.string.downloading_file);

        new Thread(() -> {
            File output = new File(requireContext().getCacheDir(), "adrenotools-" + System.currentTimeMillis() + ".zip");
            boolean downloaded = Downloader.downloadFile(asset.downloadUrl, output);
            activity.runOnUiThread(() -> {
                preloaderDialog.closeOnUiThread();
                if (!downloaded) {
                    AppUtils.showToast(getContext(), R.string.unable_to_download_file);
                    return;
                }
                installDriverUri(Uri.fromFile(output));
            });
        }).start();
    }

    private List<RemoteDriverAsset> fetchRemoteDriverAssets() {
        ArrayList<RemoteDriverAsset> assets = new ArrayList<>();
        String json = Downloader.downloadString(GITHUB_RELEASES_API);
        if (json == null || json.isEmpty()) {
            return assets;
        }

        try {
            JSONArray releases = new JSONArray(json);
            for (int i = 0; i < releases.length(); i++) {
                JSONObject release = releases.getJSONObject(i);
                if (release.optBoolean("draft", false)) {
                    continue;
                }

                String tag = release.optString("tag_name", "");
                JSONArray releaseAssets = release.optJSONArray("assets");
                if (releaseAssets == null) {
                    continue;
                }

                for (int j = 0; j < releaseAssets.length(); j++) {
                    JSONObject asset = releaseAssets.getJSONObject(j);
                    String name = asset.optString("name", "");
                    String url = asset.optString("browser_download_url", "");
                    if (name.toLowerCase().endsWith(".zip") && !url.isEmpty()) {
                        assets.add(new RemoteDriverAsset(name, tag, url));
                    }
                }
            }
        } catch (Exception ignored) {
            assets.clear();
        }

        return assets;
    }

    private static class RemoteDriverAsset {
        final String label;
        final String version;
        final String downloadUrl;

        RemoteDriverAsset(String label, String version, String downloadUrl) {
            this.label = label;
            this.version = version;
            this.downloadUrl = downloadUrl;
        }
    }

    private class RemoteDriversAdapter extends RecyclerView.Adapter<RemoteDriversAdapter.ViewHolder> {
        private final ArrayList<RemoteDriverAsset> items;

        class ViewHolder extends RecyclerView.ViewHolder {
            private final TextView tvName;
            private final TextView tvVersion;
            private final ImageButton btDownload;

            ViewHolder(View v) {
                super(v);
                tvName = v.findViewById(R.id.TVName);
                tvVersion = v.findViewById(R.id.TVVersion);
                btDownload = v.findViewById(R.id.BTDownload);
            }
        }

        RemoteDriversAdapter(ArrayList<RemoteDriverAsset> items) {
            this.items = items;
        }

        void setItems(List<RemoteDriverAsset> remoteAssets) {
            items.clear();
            items.addAll(remoteAssets);
            notifyDataSetChanged();
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.adrenotools_remote_list_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            final RemoteDriverAsset asset = items.get(position);
            holder.tvName.setText(asset.label);
            holder.tvVersion.setText(asset.version);
            holder.btDownload.setOnClickListener(v -> downloadAndInstallRemoteDriver(asset));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }

    private class DriversAdapter extends RecyclerView.Adapter<DriversAdapter.ViewHolder> {
        private ArrayList<String> driversList;

        public class ViewHolder extends RecyclerView.ViewHolder {
            private TextView tvName;
            private TextView tvVersion;
            private ImageButton btMenu;

            public ViewHolder(View v) {
                super(v);
                tvName = v.findViewById(R.id.TVName);
                tvVersion = v.findViewById(R.id.TVVersion);
                btMenu = v.findViewById(R.id.BTMenu);
            }
        }

        public DriversAdapter(ArrayList<String> driversList) {
            this.driversList = driversList;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.adrenotools_list_item, viewGroup, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, final int position) {
            viewHolder.tvName.setText(adrenotoolsManager.getDriverName(driversList.get(position)));
            viewHolder.tvVersion.setText(adrenotoolsManager.getDriverVersion(driversList.get(position)));
            viewHolder.btMenu.setOnClickListener(v -> removeAtIndex(position));
        }

        public void addItem(String item) {
            driversList.add(item);
            notifyItemInserted(getItemCount() - 1);
        }

        public void removeAtIndex(int index) {
            String deletedDriver = driversList.remove(index);
            adrenotoolsManager.removeDriver(deletedDriver);
            notifyItemRemoved(index);
            notifyItemRangeChanged(index, getItemCount());
        }

        @Override
        public int getItemCount() {
            return driversList.size();
        }
    }
}
