/**
 * Copyright 2012 multibit.org
 *
 * Licensed under the MIT license (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://opensource.org/licenses/mit-license.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.multibit.viewsystem.swing.action;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.util.Locale;

import junit.framework.TestCase;

import org.junit.Test;
import org.multibit.Localiser;
import org.multibit.controller.MultiBitController;
import org.multibit.crypto.EncryptableWallet;
import org.multibit.file.FileHandler;
import org.multibit.model.MultiBitModel;
import org.multibit.model.PerWalletModelData;
import org.multibit.model.WalletInfo;
import org.multibit.model.WalletVersion;
import org.multibit.viewsystem.swing.view.CreateNewReceivingAddressDialog;
import org.multibit.viewsystem.swing.view.components.FontSizer;

import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.NetworkParameters;

public class CreateNewReceivingAddressSubmitActionTest extends TestCase {   
    
    public static final char[] TEST_PASSWORD1 = "my hovercraft has eels".toCharArray();
    
    @Test
    public void testAddReceivingAddressesWithNonEncryptedWallet() throws Exception { 
        // Check if headless.
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        
        // Create MultiBit controller.
        MultiBitController controller = createController();
        
        // Create a new wallet and put it in the model as the active wallet.
        createNewActiveWallet(controller, "testAddReceivingAddressesWithNonEncryptedWallet", false, null);

        // Create a new CreateNewReceivingAddressSubmitAction to test.
        FontSizer.INSTANCE.initialise(controller);
        CreateNewReceivingAddressDialog createNewDialog = new CreateNewReceivingAddressDialog(controller, null, null);
        CreateNewReceivingAddressSubmitAction createNewAction = createNewDialog.getCreateNewReceivingAddressSubmitAction();

        assertNotNull("createNewAction was not created successfully", createNewAction);
        assertEquals("Wrong number of keys at wallet creation", 1, controller.getModel().getActiveWallet().getKeychain().size());
        
        // Execute the createNewAction - by default the createNewDialog should be set to add one key.
        createNewAction.actionPerformed(null);
        assertEquals("Wrong number of keys after addition of default number of keys", 2, controller.getModel().getActiveWallet().getKeychain().size());    
        
        // Add one address by selecting on the combo box.
        createNewDialog.getNumberOfAddresses().setSelectedItem(new Integer(1));
        createNewAction.actionPerformed(null);
        assertEquals("Wrong number of keys after addition of 1 key", 3, controller.getModel().getActiveWallet().getKeychain().size());
        
        // Add five addresses by selecting on the combo box.
        createNewDialog.getNumberOfAddresses().setSelectedItem(new Integer(5));
        createNewAction.actionPerformed(null);
        assertEquals("Wrong number of keys after addition of 5 keys", 8, controller.getModel().getActiveWallet().getKeychain().size());   
        
        // Add twenty addresses by selecting on the combo box.
        createNewDialog.getNumberOfAddresses().setSelectedItem(new Integer(20));
        createNewAction.actionPerformed(null);
        assertEquals("Wrong number of keys after addition of 20 keys", 28, controller.getModel().getActiveWallet().getKeychain().size());  
        
        // Add one hundred addresses by selecting on the combo box.
        createNewDialog.getNumberOfAddresses().setSelectedItem(new Integer(100));
        createNewAction.actionPerformed(null);
        assertEquals("Wrong number of keys after addition of 100 keys", 128, controller.getModel().getActiveWallet().getKeychain().size());    
    }
    
    @Test
    public void testAddReceivingAddressesWithEncryptedWallet() throws Exception {   
        // Check if headless.
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        
        // Create MultiBit controller.
        MultiBitController controller = createController();
        
        // Create a new encrypted wallet and put it in the model as the active wallet.
        createNewActiveWallet(controller, "testAddReceivingAddressesWithEncryptedWallet", true, TEST_PASSWORD1);

        // Create a new CreateNewReceivingAddressSubmitAction to test.
        FontSizer.INSTANCE.initialise(controller);
        CreateNewReceivingAddressDialog createNewDialog = new CreateNewReceivingAddressDialog(controller, null, null);
        CreateNewReceivingAddressSubmitAction createNewAction = createNewDialog.getCreateNewReceivingAddressSubmitAction();

        assertNotNull("createNewAction was not created successfully", createNewAction);
        assertEquals("Wrong number of keys at wallet creation", 1, controller.getModel().getActiveWallet().getKeychain().size());
        
        // Execute the createNewAction - by default the createNewDialog sould be set to add one key.
        // However as there is no wallet password supplied it will not add the key.
        createNewAction.actionPerformed(null);
        assertEquals("Wrong number of keys after addition of default number of keys with no wallet password", 1, controller.getModel().getActiveWallet().getKeychain().size());    
        
        // Check there is a message that the wallet password is required.
        assertEquals("No message to enter wallet password", "Enter the wallet password", createNewDialog.getMessageText());
        
        // Enter an incorrect password. There should be a message to the user.
        createNewDialog.setWalletPassword("This is the wrong password");
        createNewAction.actionPerformed(null);

        // Check there is a message that the wallet password isincorrect.
        assertEquals("No message to that wallet password is incorrect", "The wallet password is incorrect", createNewDialog.getMessageText());

        // Set the correct wallet password.
        createNewDialog.setWalletPassword(new String(TEST_PASSWORD1));
        
        // The new private key should now add correctly.
        createNewAction.actionPerformed(null);
        assertEquals("Wrong number of keys after addition of default number of keys with wallet password", 2, controller.getModel().getActiveWallet().getKeychain().size()); 

        // Add twenty addresses by selecting on the combo box.
        createNewDialog.getNumberOfAddresses().setSelectedItem(new Integer(20));
        createNewDialog.setWalletPassword(new String(TEST_PASSWORD1));
        createNewAction.actionPerformed(null);
        assertEquals("Wrong number of keys after addition of 20 keys", 22, controller.getModel().getActiveWallet().getKeychain().size());  

        // The added private keys should be encrypted with the same password as the wallet password.
        // Thus a decrypt should work fine.
        ((EncryptableWallet)controller.getModel().getActiveWallet()).decrypt(TEST_PASSWORD1);
    }
    
    private MultiBitController createController() {
       MultiBitController controller = new MultiBitController();
        
        Localiser localiser = new Localiser(Locale.ENGLISH);
        MultiBitModel model = new MultiBitModel(controller);
        
        controller.setLocaliser(localiser);
        controller.setModel(model);
        
        return controller;
    }
    
    private void createNewActiveWallet(MultiBitController controller, String descriptor, boolean encrypt, char[] walletPassword) throws Exception {
        EncryptableWallet wallet = new EncryptableWallet(NetworkParameters.prodNet());
        wallet.getKeychain().add(new ECKey());
 
        PerWalletModelData perWalletModelData = new PerWalletModelData();
        perWalletModelData.setWallet(wallet);
 
        // Save the wallet to a temporary directory.
        File multiBitDirectory = FileHandler.createTempDirectory("CreateAndDeleteWalletsTest");
        String multiBitDirectoryPath = multiBitDirectory.getAbsolutePath();
        String walletFile = multiBitDirectoryPath + File.separator + descriptor + ".wallet";
        
        // Put the wallet in the model as the active wallet.
        perWalletModelData.setWalletInfo(new WalletInfo(walletFile, WalletVersion.PROTOBUF));
        perWalletModelData.setWalletFilename(walletFile);
        perWalletModelData.setWalletDescription(descriptor);
        
        // Save the wallet and load it up again, making it the active wallet.
        // This also sets the timestamp fields used in file change detection.
        FileHandler fileHandler = new FileHandler(controller);
        fileHandler.savePerWalletModelData(perWalletModelData, true);
        PerWalletModelData loadedPerWalletModelData = fileHandler.loadFromFile(new File(walletFile));
            
        if (encrypt) {
            ((EncryptableWallet)loadedPerWalletModelData.getWallet()).encrypt(walletPassword);
        }

        controller.getModel().addAndMakeActiveWallet(loadedPerWalletModelData);

        assertEquals("The test wallet " + descriptor + " was not created properly", walletFile, controller.getModel().getActiveWalletFilename());
    }
}
