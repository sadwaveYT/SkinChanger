/*
 *     Copyright (C) 2020 boomboompower
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package wtf.boomy.mods.skinchanger.gui.additional;

import net.minecraft.client.Minecraft;

import org.apache.commons.codec.digest.DigestUtils;

import wtf.boomy.mods.skinchanger.SkinChangerMod;
import wtf.boomy.mods.skinchanger.api.SkinAPIType;
import wtf.boomy.mods.skinchanger.cosmetic.PlayerSkinType;
import wtf.boomy.mods.skinchanger.cosmetic.impl.fakeplayer.FakePlayerRender;
import wtf.boomy.mods.skinchanger.gui.SkinChangerMenu;
import wtf.boomy.mods.skinchanger.gui.StringSelectionType;
import wtf.boomy.mods.skinchanger.utils.ChatColor;
import wtf.boomy.mods.skinchanger.utils.backend.CacheRetriever;
import wtf.boomy.mods.skinchanger.utils.backend.ThreadFactory;
import wtf.boomy.mods.skinchanger.utils.gui.impl.ModernButton;
import wtf.boomy.mods.skinchanger.utils.gui.impl.ModernTextBox;

import java.awt.Color;
import java.util.Objects;

public class PlayerSelectMenu extends SkinChangerMenu {
    
    private static boolean loading;
    
    private final CacheRetriever cacheRetriever;
    private final FakePlayerRender fakePlayerRender;
    private final ThreadFactory threadFactory;
    
    private SkinAPIType skinAPI;
    private SkinChangerMenu skinChangerMenu;
    private StringSelectionType selectionType;
    
    private float errorMessageTimer = 0;
    private String lastErrorMessage = null;
    private String errorMessage = "";
    
    private ModernButton skinTypeButton;
    private ModernTextBox textBox;
    
    public PlayerSelectMenu(SkinChangerMenu menu, StringSelectionType selectionType) {
        this.skinChangerMenu = menu;
        this.selectionType = selectionType;
        
        this.skinAPI = SkinChangerMod.getInstance().getConfig().getSkinAPIType();
        this.fakePlayerRender = SkinChangerMod.getInstance().getCosmeticFactory().getFakePlayerRender();
        this.cacheRetriever = SkinChangerMod.getInstance().getCacheRetriever();
        this.threadFactory = new ThreadFactory("SelectionMenu");
    }
    
    @Override
    protected void onGuiInitExtra() {
        setAsSubMenu(this.skinChangerMenu);
    
        this.skinAPI = SkinChangerMod.getInstance().getConfig().getSkinAPIType();
        
        float boxWidth = 150;
        float boxHeight = 20;
        
        float xLocation = ((float) this.width / 4) - (boxWidth / 2);
        float yLocation = ((float) this.height / 2) - boxHeight;
        
        ModernTextBox entryBox = new ModernTextBox(0, (int) xLocation, (int) yLocation, (int) boxWidth, (int) boxHeight);
        
        if (this.selectionType.isTypeOfUrl()) {
            entryBox.setMaxStringLength(520);
        } else if (this.selectionType.isTypeOfUUID()) {
            entryBox.setMaxStringLength(36);
        } else {
            entryBox.setMaxStringLength(16);
        }
        
        registerElement(entryBox);
        
        this.textBox = entryBox;
        
        yLocation += boxHeight + 4;
    
        ModernButton loadButton = new ModernButton(500, (int) xLocation, (int) yLocation, (int) boxWidth, (int) boxHeight, "Load", this::onLoadClicked);
        
        yLocation += loadButton.getHeight() + 20;
        
        if (this.selectionType.isTypeOfSkin()) {
            ModernButton type = new ModernButton(505, (int) xLocation, (int) yLocation, (int) boxWidth, (int) boxHeight, "Type: " + ChatColor.AQUA + this.fakePlayerRender.getSkinType().getDisplayName(), this::onTypeClicked);
    
            type.setEnabled(this.mod.getStorage().isSkinTypePatchApplied());
            
            yLocation += loadButton.getHeight() + 4;
    
            registerElement(type);
            
            this.skinTypeButton = type;
        }
    
        ModernButton confirm = new ModernButton(506, (int) xLocation, (int) yLocation, (int) boxWidth, (int) boxHeight, "Confirm", button -> this.skinChangerMenu.display());
    
        registerElement(loadButton);
        registerElement(confirm);
    }
    
    @Override
    public void onRender(int mouseX, int mouseY, float partialTicks) {
        super.onRender(mouseX, mouseY, partialTicks);
        
        if (!Objects.equals(this.lastErrorMessage, this.errorMessage)) {
            System.out.println(this.errorMessage);
            
            this.errorMessageTimer = 0;
            
            this.lastErrorMessage = errorMessage;
        }
        
        int floatingPosition = (int) cap(this.errorMessageTimer);
        
        drawCenteredString(this.fontRendererObj, this.selectionType.getDisplaySentence(), this.width / 4, this.height / 2 - 40, Color.WHITE.getRGB());
        
        drawCenteredString(this.fontRendererObj, this.errorMessage, this.width / 2, this.height - floatingPosition, Color.WHITE.getRGB());
        
        this.errorMessageTimer += partialTicks;
    }
    
    @Override
    public void buttonPressed(ModernButton button) {
        if (button.getId() == 55) {
            this.skinChangerMenu.display();
        }
    }
    
    private void onTypeClicked(ModernButton button) {
        PlayerSkinType nextType = this.fakePlayerRender.getSkinType().getNextSkin();
        
        this.fakePlayerRender.setRawSkinType(nextType);
        
        button.setText("Type: " + ChatColor.AQUA + nextType.getDisplayName());
    }
    
    private void onLoadClicked(ModernButton button) {
        if (Minecraft.getMinecraft().isCallingFromMinecraftThread()) {
            this.threadFactory.runAsync(() -> {
                loading = true;
    
                onLoadClicked(button);
            
                loading = false;
            });
        
            return;
        }
        
        // Should never happen.
        if (this.textBox == null) {
            return;
        }
    
        String enteredText = this.textBox.getText().trim();
    
        if (enteredText.isEmpty()) {
            return;
        }
    
        if (!this.selectionType.isValid(enteredText)) {
            String errorText = "";
        
            if (this.selectionType.isTypeOfUrl()) {
                this.errorMessage = "The entered value did not start with https:// or http://";
            } else if (this.selectionType.isTypeOfUsername()) {
                this.errorMessage = "The entered value was larger than valid username's";
            }
        
            this.errorMessage = errorText;
        
            return;
        }
    
        if (button.getId() == 500) {
            handleSelectionPress(enteredText);
        }
    }
    
    private void handleSelectionPress(String enteredText) {
        String cacheName = this.selectionType.name().charAt(0) + enteredText;
        
        if (this.selectionType.isTypeOfUrl()) {
            cacheName = DigestUtils.md5Hex(cacheName.getBytes());
        } else if (this.selectionType.isTypeOfUUID() && enteredText.contains("-")) {
            enteredText = enteredText.replace("-", "");
        }
        
        switch (this.selectionType) {
            case P_USERNAME:
                String realUsername = this.skinAPI.getAPI().getRealNameFromName(enteredText);
                
                this.skinAPI.getAPI().getSkinFromId(this.skinAPI.getAPI().getIdFromUsername(realUsername), resourceLocation -> {
                    boolean hasSlimSkin = this.skinAPI.getAPI().hasSlimSkin(realUsername);
    
                    this.fakePlayerRender.setSkinType(hasSlimSkin ? "slim" : "default");
                    this.fakePlayerRender.setSkinLocation(resourceLocation);
                    
                    if (this.skinTypeButton != null) {
                        this.skinTypeButton.setText("Type: " + ChatColor.AQUA + this.fakePlayerRender.getSkinType().getDisplayName());
                    }
                });
                
                break;
            case C_USERNAME:
                String url = "http://s.optifine.net/capes/" + enteredText + ".png";
            
                this.cacheRetriever.loadIntoGame(cacheName, url, this.selectionType.getCacheType(), this.fakePlayerRender::setCapeLocation);
            
                break;
            case P_URL:
                // Skin URL Resource
                this.cacheRetriever.loadIntoGame(cacheName, enteredText, this.selectionType.getCacheType(), this.fakePlayerRender::setSkinLocation);
                
                break;
            case C_URL:
                // Cape URL Resource
                this.cacheRetriever.loadIntoGame(cacheName, enteredText, this.selectionType.getCacheType(), this.fakePlayerRender::setCapeLocation);
            
                break;
            case P_UUID:
                // Skin UUID Resource
                this.skinAPI.getAPI().getSkinFromId(enteredText, this.fakePlayerRender::setSkinLocation);
                
                break;
            case C_UUID:
                // Cape UUID Resource
                this.cacheRetriever.loadIntoGame(enteredText, "http://s.optifine.net/capes/" + this.skinAPI.getAPI().getNameFromID(enteredText) + ".png", this.selectionType.getCacheType(), this.fakePlayerRender::setCapeLocation);
        }
    }
    
    /**
     * Caps this integer between a few magic numbers
     *
     * @param in the number to cap
     *
     * @return a number between 0 and 30.
     */
    private float cap(float in) {
        if (in < 0) {
            return 0;
        }
        
        // 30 is a magic number
        return Math.max(in, 30);
    }
    
    /**
     * Extra things
     *
     * @param parentMenu the SkinChanger menu
     * @param type       the type which it should be switched to
     */
    public void displayExtra(SkinChangerMenu parentMenu, StringSelectionType type) {
        this.skinChangerMenu = parentMenu;
        
        this.selectionType = type;
        
        display();
    }
}
