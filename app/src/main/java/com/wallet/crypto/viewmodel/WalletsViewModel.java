package com.wallet.crypto.viewmodel;

import android.app.Activity;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;

import com.wallet.crypto.TrustConstants;
import com.wallet.crypto.entity.ErrorEnvelope;
import com.wallet.crypto.entity.Wallet;
import com.wallet.crypto.interact.CreateWalletInteract;
import com.wallet.crypto.interact.DeleteWalletInteract;
import com.wallet.crypto.interact.ExportWalletInteract;
import com.wallet.crypto.interact.FetchWalletsInteract;
import com.wallet.crypto.interact.FindDefaultWalletInteract;
import com.wallet.crypto.interact.SetDefaultWalletInteract;
import com.wallet.crypto.router.ImportWalletRouter;
import com.wallet.crypto.router.TransactionsRouter;

import static com.wallet.crypto.TrustConstants.IMPORT_REQUEST_CODE;

public class WalletsViewModel extends BaseViewModel {

	private final CreateWalletInteract createWalletInteract;
	private final SetDefaultWalletInteract setDefaultWalletInteract;
	private final DeleteWalletInteract deleteWalletInteract;
	private final FetchWalletsInteract fetchWalletsInteract;
	private final FindDefaultWalletInteract findDefaultWalletInteract;
    private final ExportWalletInteract exportWalletInteract;

	private final ImportWalletRouter importWalletRouter;
    private final TransactionsRouter transactionsRouter;

	private final MutableLiveData<Wallet[]> wallets = new MutableLiveData<>();
	private final MutableLiveData<Wallet> defaultWallet = new MutableLiveData<>();
	private final MutableLiveData<Wallet> createdWallet = new MutableLiveData<>();
	private final MutableLiveData<ErrorEnvelope> createWalletError = new MutableLiveData<>();
	private final MutableLiveData<String> exportedStore = new MutableLiveData<>();

    WalletsViewModel(
            CreateWalletInteract createWalletInteract,
            SetDefaultWalletInteract setDefaultWalletInteract,
            DeleteWalletInteract deleteWalletInteract,
            FetchWalletsInteract fetchWalletsInteract,
            FindDefaultWalletInteract findDefaultWalletInteract,
            ExportWalletInteract exportWalletInteract,
            ImportWalletRouter importWalletRouter,
            TransactionsRouter transactionsRouter) {
		this.createWalletInteract = createWalletInteract;
		this.setDefaultWalletInteract = setDefaultWalletInteract;
		this.deleteWalletInteract = deleteWalletInteract;
		this.fetchWalletsInteract = fetchWalletsInteract;
		this.findDefaultWalletInteract = findDefaultWalletInteract;
		this.importWalletRouter = importWalletRouter;
		this.exportWalletInteract = exportWalletInteract;
		this.transactionsRouter = transactionsRouter;

		fetchWallets();
	}

	public LiveData<Wallet[]> wallets() {
		return wallets;
	}

	public LiveData<Wallet> defaultWallet() {
		return defaultWallet;
	}

    public LiveData<Wallet> createdWallet() {
        return createdWallet;
    }

    public LiveData<String> exportedStore() {
        return exportedStore;
    }


	public void setDefaultWallet(Wallet wallet) {
		setDisposable(setDefaultWalletInteract
                .set(wallet)
                .subscribe(() -> onDefaultWalletChanged(wallet), this::onError));
	}

	public void deleteWallet(Wallet wallet) {
		setDisposable(deleteWalletInteract
                .delete(wallet)
                .subscribe(this::onFetchWallets, this::onError));
	}

	private void onFetchWallets(Wallet[] items) {
		getProgress().postValue(false);
		wallets.postValue(items);
		setDisposable(findDefaultWalletInteract
                .find()
                .subscribe(this::onDefaultWalletChanged, t -> {
                }));
	}

	private void onDefaultWalletChanged(Wallet wallet) {
		getProgress().postValue(false);
		defaultWallet.postValue(wallet);
	}

	public void fetchWallets() {
		getProgress().postValue(true);
		setDisposable(fetchWalletsInteract
                .fetch()
                .subscribe(this::onFetchWallets, this::onError));
	}

	public void newWallet() {
		getProgress().setValue(true);
		createWalletInteract
				.create()
				.subscribe(account -> {
					fetchWallets();
					createdWallet.postValue(account);
				}, this::onCreateWalletError);
	}

    public void exportWallet(Wallet wallet, String oldPassword) {
        exportWalletInteract
                .export(wallet, oldPassword)
                .subscribe(exportedStore::postValue, this::onExportError);
    }

    private void onExportError(Throwable throwable) {
        getError().postValue(new ErrorEnvelope(TrustConstants.ErrorCode.UNKNOWN, throwable.getMessage()));
    }

    private void onCreateWalletError(Throwable throwable) {
        createWalletError.postValue(new ErrorEnvelope(TrustConstants.ErrorCode.UNKNOWN, throwable.getMessage()));
	}

	public void importWallet(Activity activity) {
		importWalletRouter.openForResult(activity, IMPORT_REQUEST_CODE);
	}

    public void showTransactions(Context context) {
        transactionsRouter.open(context, true);
    }

	public void createdPwd(String pwd) {
		createWalletInteract.createdPwd(pwd);
	}
}
