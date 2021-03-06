package org.kaizoku.otropelisplusmas.ui.home.video_cartel;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.snackbar.Snackbar;

import org.kaizoku.otropelisplusmas.MainActivity;
import org.kaizoku.otropelisplusmas.R;
import org.kaizoku.otropelisplusmas.adapter.VideoServerAdapter;
import org.kaizoku.otropelisplusmas.database.entity.CapituloEnt;
import org.kaizoku.otropelisplusmas.database.entity.SerieEnt;
import org.kaizoku.otropelisplusmas.database.viewmodel.CapituloViewModel;
import org.kaizoku.otropelisplusmas.databinding.FragmentVideoCartelBinding;
import org.kaizoku.otropelisplusmas.service.PelisplushdService;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class VideoCartelFragment extends Fragment implements VideoServerAdapter.OnCardListener{
    private static final String TAG = "flub1";
    private FragmentVideoCartelBinding binding;
    private PelisplushdService pelisplushdService;
    private VideoServerAdapter videoServerAdapter;

    private CapituloViewModel capituloViewModel;
    private SerieEnt serie;
    private CapituloEnt capitulo;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        binding = FragmentVideoCartelBinding.inflate(inflater,container,false);
        pelisplushdService = new PelisplushdService();

        capituloViewModel = new ViewModelProvider(this).get(CapituloViewModel.class);

        intiAdapterServer();

        loadArguments();

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        checkPendingCapituloProgress();
    }

    private void loadArguments() {
        Bundle b = getArguments();
        if(b!=null) {
            serie = b.getParcelable("serie");
            String url;
            if(serie==null) {
                url = b.getString("url", "");//desde serie o home
            }else{
                url = serie.getCurrentSeasonChapter().href;
            }

            capituloViewModel.getCapitulo(url)
                    .flatMap(capituloEnt -> {
                        capitulo = capituloEnt;
                        loadCapituloEntCartel(capitulo);
                        Log.i(TAG, "apply: 1 capituloEnt: "+capituloEnt);
                        return pelisplushdService.getSingleVideoCartel(url)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread());
                    })
                    .onErrorResumeNext(throwable -> {
                        return pelisplushdService.getSingleVideoCartel(url)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread());
                    })
                    .flatMap(capituloEnt -> {
                        Log.i(TAG, "apply: 3 capituloGet from web service: "+capituloEnt);
                        loadCapituloEntCartel(capituloEnt);
                        videoServerAdapter.setList(capituloEnt.videoServerList);
                        if(capitulo==null) {//no insertado
                            capitulo=capituloEnt;
                            return capituloViewModel.insertCapitolo(capituloEnt);
                        }else {
                            capitulo.videoServerList=capituloEnt.videoServerList;
                            return Single.just(-1l);
                        }
                    })
                    .subscribe((id, throwable) -> {
                        if(throwable==null){
                            Log.i(TAG, "accept: id: " + id);
                            if(id>0)
                                capitulo.id=id;
                            loadCapituloEntCartel(capitulo);
                        }else Log.e(TAG, "accept: error ", throwable);
                    });


            //todo: hacer esta llamada solo si la lista esta vacia
            /*
            pelisplushdService.getSingleVideoCartel(url)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe((capituloCartel, throwable) -> {
                        if(throwable==null&& capituloCartel !=null){
                            Log.i(TAG, "onCreateView: entro getSingleVideoCartel");
                            //videoCartelViewModel.setVideoCartel(capituloCartel);
                            //setVideoCartel(videoCartel);
                            //videoServerAdapter.setList(videoCartel.videoServerList);

                            //agregar webview aqui

                            Log.i("TAG", "onCreateView: url disqus: "+ capituloCartel.url_disqus);

                            if(capituloCartel.url_disqus!=null) {
                                // use cookies to remember a logged in status
                                //CookieSyncManager.createInstance(this);
                                //CookieSyncManager.getInstance().startSync();

                                binding.webview.setVerticalScrollBarEnabled(true);
                                binding.webview.requestFocus();
                                binding.webview.getSettings().setJavaScriptEnabled(true);
                                binding.webview.setWebViewClient(new WebViewClient());
                                binding.webview.loadUrl(capituloCartel.url_disqus);

                                //If you are using Android Lollipop i.e. SDK 21, then:
                                //CookieManager.getInstance().setAcceptCookie(true);
                                //won't work. You need to use:
                                CookieManager.getInstance().setAcceptThirdPartyCookies(binding.webview, true);

                                binding.refresh.setOnClickListener(v -> {
                                    binding.webview.loadUrl(capituloCartel.url_disqus);
                                });

                            }else{
                                //binding.webview.setVisibility(View.GONE);
                                Log.i("TAG", "onCreateView: info");
                            }
                            //binding.webview.loadUrl("https://disqus.com/embed/comments/?base=default&amp;f=https-animeflv-net&amp;t_i=episode_58508&amp;t_u=https%3A%2F%2Fwww3.animeflv.net%2Fver%2Fseijo-no-maryoku-wa-bannou-desu-10&amp;t_d=Seijo%20no%20Maryoku%20wa%20Bannou%20Desu%20Episodio%2010&amp;t_t=Seijo%20no%20Maryoku%20wa%20Bannou%20Desu%20Episodio%2010&amp;s_o=default#version=a5921af07b365f6dfd62075d2dee3735");
                        }
                    });
            */
        }
    }

    private void intiAdapterServer() {
        binding.fragSerieRvServers.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        binding.fragSerieRvServers.setLayoutManager(linearLayoutManager);
        videoServerAdapter = new VideoServerAdapter(this);//ultimo error aqui cheaquearlo
        binding.fragSerieRvServers.setAdapter(videoServerAdapter);
    }

    private void loadCapituloEntCartel(CapituloEnt capitulo){
        if(capitulo.visto)
            binding.fragSerieChipVisto.setVisibility(View.VISIBLE);
        else binding.fragSerieChipVisto.setVisibility(View.GONE);
        ((MainActivity)getActivity()).setDisplayShowTitleEnabled(true);
        getActivity().setTitle(capitulo.titulo);
        binding.fragSerieSinopsis.setText(capitulo.sinopsis);
        Log.i(TAG, "loadCapituloEntCartel: size: "+capitulo.videoServerList.size());
        videoServerAdapter.setList(capitulo.videoServerList);

        //configurando el webview con comentarios
        if(capitulo.url_disqus!=null) {
            // use cookies to remember a logged in status
            //CookieSyncManager.createInstance(this);
            //CookieSyncManager.getInstance().startSync();
            binding.webview.setVerticalScrollBarEnabled(true);
            binding.webview.requestFocus();
            binding.webview.getSettings().setJavaScriptEnabled(true);
            binding.webview.setWebViewClient(new WebViewClient());
            binding.webview.loadUrl(capitulo.url_disqus);
            //If you are using Android Lollipop i.e. SDK 21, then:
            //CookieManager.getInstance().setAcceptCookie(true);
            //won't work. You need to use:
            CookieManager.getInstance().setAcceptThirdPartyCookies(binding.webview, true);
            binding.refresh.setOnClickListener(v -> binding.webview.loadUrl(capitulo.url_disqus));
        }
        try {
            ((MainActivity)getActivity()).loadImgToolbar(capitulo.src_img);
        }catch (Exception e){e.printStackTrace();}
    }

    @Override
    public void onClickCard(String file_url,byte option) {
        capitulo.visto=true;
        capituloViewModel.updateCapitulo(capitulo).subscribe();
        switch (option) {
            case VideoServerAdapter.OPTION_PLAY:
                Bundle b=new Bundle();
                if(serie!=null)
                    b.putParcelable("serie",serie);
                capitulo.file_url=file_url;
                b.putParcelable("capitulo",capitulo);
                NavHostFragment.findNavController(this)
                        .navigate(R.id.action_videoCartelFragment_to_nav_reproductor,b);
                break;
            case VideoServerAdapter.OPTION_EXT:
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse(file_url), "video/*");
                startActivity(Intent.createChooser(intent, "Play Video"));
                break;
            case VideoServerAdapter.OPTION_CAST:
                Snackbar.make(getView(),"Proximamente",Snackbar.LENGTH_LONG).show();
                break;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ((MainActivity)getActivity()).setDisplayShowTitleEnabled(false);
    }

    private void checkPendingCapituloProgress() {
        Log.i(TAG, "checkPendingCapituloProgress: checando shared");
        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
        long progress = sharedPref.getLong("progress",0);
        long capitulo_id = sharedPref.getLong("capitulo_id",0);

        Log.i(TAG, "checkPendingCapituloProgress: progress: "+progress);
        if(progress>0){
            CapituloViewModel capituloViewModel;
            capituloViewModel = new ViewModelProvider(this).get(CapituloViewModel.class);

            capituloViewModel.getCapitulo(capitulo_id)
                    .flatMap(capituloEnt -> {
                        Log.i(TAG, "checkPendingCapituloProgress: capituloEnt: "+capituloEnt);
                        capituloEnt.progress = progress;
                        capitulo=capituloEnt;
                        return capituloViewModel.updateCapitulo(capituloEnt);
                    }).subscribe((integer, throwable) -> {
                        Log.i(TAG, "checkPendingCapituloProgress: integer: "+integer);
                        if(throwable==null) {
                            SharedPreferences.Editor editor = sharedPref.edit();
                            editor.remove("progress");
                            editor.remove("capitulo_id");
                            editor.commit();
                            Log.i(TAG, "checkPendingCapituloProgress: capitulo pendiente progreso actualizado con exito");
                        }
                    });
        }
    }
}
