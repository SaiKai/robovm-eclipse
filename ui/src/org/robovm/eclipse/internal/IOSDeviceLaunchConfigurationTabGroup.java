/*
 * Copyright (C) 2012 Trillian AB
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/gpl-2.0.html>.
 */
package org.robovm.eclipse.internal;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.robovm.compiler.target.ios.ProvisioningProfile;
import org.robovm.compiler.target.ios.SDK;
import org.robovm.compiler.target.ios.SigningIdentity;
import org.robovm.compiler.target.ios.IOSSimulatorLaunchParameters.Family;
import org.robovm.eclipse.RoboVMPlugin;

/**
 * @author niklas
 *
 */
public class IOSDeviceLaunchConfigurationTabGroup extends AbstractLaunchConfigurationTabGroup {

    @Override
    public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
        setTabs(new ILaunchConfigurationTab[] {
                new DeviceTab(),
                new CommonTab()
        });
    }

    public static class DeviceTab extends RoboVMTab {

        private List<SigningIdentity> signingIdentities;
        private List<ProvisioningProfile> provisioningProfiles;
        private Combo signingIdCombo;
        private Combo profileCombo;
        
        public DeviceTab() {
        }
        
        private String[] readSigningIdentities() {
            String[] result = new String[signingIdentities.size() + 2];
            int i = 0;
            result[i++] = "Auto (starts with 'iPhone Developer')";
            result[i++] = "Skip Signing";
            for (SigningIdentity sid : signingIdentities) {
                result[i++] = sid.getName();
            }
            return result;
        }
        
        private String[] readProvisioningProfiles() {
            String[] result = new String[provisioningProfiles.size() + 1];
            int i = 0;
            result[i++] = "Auto";
            for (ProvisioningProfile p : provisioningProfiles) {
                String appId = p.getEntitlements().objectForKey("application-identifier").toString();
                result[i++] = p.getName() + " (" + appId + ")";
            }
            return result;
        }
        
        @Override
        public void createControl(Composite parent) {
            Composite root = createRoot(parent);
            createProjectEditor(root);
            createDeviceEditor(root);
            setControl(root);
        }
        
        protected void createDeviceEditor(Composite parent) {
            Group group = new Group(parent, SWT.NONE);
            group.setText("iOS Device:");
            group.setFont(group.getFont());
            group.setLayout(new GridLayout(2, false));
            group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            ((GridData) group.getLayoutData()).horizontalSpan = 1;
            ((GridLayout) group.getLayout()).verticalSpacing = 0;
            
            Label signingIdLabel = new Label(group, SWT.NONE);
            signingIdLabel.setFont(group.getFont());
            signingIdLabel.setText("Signing identity:");
            signingIdLabel.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, false, false));

            signingIdCombo = new Combo(group, SWT.READ_ONLY | SWT.BORDER);
            signingIdCombo.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false));
            signingIdCombo.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent event) {
                    updateLaunchConfigurationDialog();
                    if (signingIdCombo.getSelectionIndex() == 1){
                        profileCombo.setEnabled(false);
                    }else {
                        profileCombo.setEnabled(true);
                    }
                }
            });

            Label profileLabel = new Label(group, SWT.NONE);
            profileLabel.setFont(group.getFont());
            profileLabel.setText("Provisioning profile:");
            profileLabel.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, false, false));

            profileCombo = new Combo(group, SWT.READ_ONLY | SWT.BORDER);
            profileCombo.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false));
            profileCombo.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent event) {
                    updateLaunchConfigurationDialog();
                }
            });

            setControl(group);
        }

        @Override
        public void initializeFrom(ILaunchConfiguration config) {
            super.initializeFrom(config);

            signingIdentities = SigningIdentity.list();
            provisioningProfiles = ProvisioningProfile.list();
            signingIdCombo.setItems(readSigningIdentities());
            signingIdCombo.select(0);
            profileCombo.setItems(readProvisioningProfiles());
            profileCombo.select(0);
            
            try {
                String v = config.getAttribute(IOSDeviceLaunchConfigurationDelegate.ATTR_IOS_DEVICE_SIGNING_ID, (String) null);
                int idx = -1;
                if (v != null) {
                    try {
                        SigningIdentity signingIdentity = SigningIdentity.find(signingIdentities, v);
                        idx = signingIdentities.indexOf(signingIdentity);
                    } catch (IllegalArgumentException e) {
                        // Ignore
                    }
                }
                if (idx != -1) {
                    signingIdCombo.select(idx + 1);
                }
            } catch (CoreException e) {
                RoboVMPlugin.log(e);
            }
            try {
                String v = config.getAttribute(IOSDeviceLaunchConfigurationDelegate.ATTR_IOS_DEVICE_PROVISIONING_PROFILE, (String) null);
                int idx = -1;
                if (v != null) {
                    try {
                        ProvisioningProfile profile = ProvisioningProfile.find(provisioningProfiles, v);
                        idx = provisioningProfiles.indexOf(profile);
                    } catch (IllegalArgumentException e) {
                        // Ignore
                    }
                }
                if (idx != -1) {
                    profileCombo.select(idx + 1);
                }
            } catch (CoreException e) {
                RoboVMPlugin.log(e);
            }
        }

        @Override
        public void performApply(ILaunchConfigurationWorkingCopy wc) {
            super.performApply(wc);
            int signingIdIndex = signingIdCombo.getSelectionIndex();

            if (signingIdIndex == 1){
                wc.setAttribute(IOSDeviceLaunchConfigurationDelegate.ATTR_IOS_DEVICE_SKIP_SIGNING,
                        true);
            }else{
                SigningIdentity signingId = signingIdIndex == 0 ? null : signingIdentities.get(signingIdIndex - 1);
                wc.setAttribute(IOSDeviceLaunchConfigurationDelegate.ATTR_IOS_DEVICE_SIGNING_ID,
                        signingId != null ? signingId.getFingerprint() : null);
                int profileIndex = profileCombo.getSelectionIndex();
                ProvisioningProfile profile = profileIndex == 0 ? null : provisioningProfiles.get(profileIndex - 1);
                wc.setAttribute(IOSDeviceLaunchConfigurationDelegate.ATTR_IOS_DEVICE_PROVISIONING_PROFILE,
                        profile != null ? profile.getUuid() : null);
                wc.setAttribute(IOSDeviceLaunchConfigurationDelegate.ATTR_IOS_DEVICE_SKIP_SIGNING,
                        false);
            }
        }

        @Override
        public void setDefaults(ILaunchConfigurationWorkingCopy wc) {
            super.setDefaults(wc);
            wc.setAttribute(IOSDeviceLaunchConfigurationDelegate.ATTR_IOS_DEVICE_SIGNING_ID, (String) null);
            wc.setAttribute(IOSDeviceLaunchConfigurationDelegate.ATTR_IOS_DEVICE_SIGNING_ID, (String) null);
            wc.setAttribute(IOSDeviceLaunchConfigurationDelegate.ATTR_IOS_DEVICE_SKIP_SIGNING, false);
        }
        
    }
}
