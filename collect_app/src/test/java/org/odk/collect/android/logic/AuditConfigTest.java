/*
 * Copyright 2018 Nafundi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.redrosecps.collect.android.logic;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static com.redrosecps.collect.android.location.client.LocationClient.Priority.PRIORITY_BALANCED_POWER_ACCURACY;
import static com.redrosecps.collect.android.location.client.LocationClient.Priority.PRIORITY_HIGH_ACCURACY;
import static com.redrosecps.collect.android.location.client.LocationClient.Priority.PRIORITY_LOW_POWER;
import static com.redrosecps.collect.android.location.client.LocationClient.Priority.PRIORITY_NO_POWER;

public class AuditConfigTest {

    @Test
    public void testParameters() {
        AuditConfig auditConfig = new AuditConfig("high-accuracy", "10", "60", true);

        assertTrue(auditConfig.isTrackingChangesEnabled());
        assertTrue(auditConfig.isLocationEnabled());
        assertEquals(PRIORITY_HIGH_ACCURACY, auditConfig.getLocationPriority());
        assertEquals(10000, auditConfig.getLocationMinInterval().intValue());
        assertEquals(60000, auditConfig.getLocationMaxAge().intValue());

        auditConfig = new AuditConfig("high-accuracy", "0", "60", false);

        assertFalse(auditConfig.isTrackingChangesEnabled());
        assertTrue(auditConfig.isLocationEnabled());
        assertEquals(PRIORITY_HIGH_ACCURACY, auditConfig.getLocationPriority());
        assertEquals(1000, auditConfig.getLocationMinInterval().intValue());
        assertEquals(60000, auditConfig.getLocationMaxAge().intValue());
    }

    @Test
    public void logLocationCoordinatesOnlyIfAllParametersAreSet() {
        AuditConfig auditConfig = new AuditConfig("high-accuracy", "10", "60", false);
        assertTrue(auditConfig.isLocationEnabled());
        auditConfig = new AuditConfig(null, "10", "60", false);
        assertFalse(auditConfig.isLocationEnabled());
        auditConfig = new AuditConfig(null, null, "60", false);
        assertFalse(auditConfig.isLocationEnabled());
        auditConfig = new AuditConfig(null, null, null, false);
        assertFalse(auditConfig.isLocationEnabled());
        auditConfig = new AuditConfig("balanced", null, null, false);
        assertFalse(auditConfig.isLocationEnabled());
        auditConfig = new AuditConfig("balanced", "10", null, false);
        assertFalse(auditConfig.isLocationEnabled());
        auditConfig = new AuditConfig("balanced", null, "60", false);
        assertFalse(auditConfig.isLocationEnabled());
        auditConfig = new AuditConfig(null, null, "60", false);
        assertFalse(auditConfig.isLocationEnabled());
    }

    @Test
    public void testPriorities() {
        AuditConfig auditConfig = new AuditConfig("high_accuracy", null, null, false);
        assertEquals(PRIORITY_HIGH_ACCURACY, auditConfig.getLocationPriority());
        auditConfig = new AuditConfig("high-accuracy", null, null, false);
        assertEquals(PRIORITY_HIGH_ACCURACY, auditConfig.getLocationPriority());
        auditConfig = new AuditConfig("HIGH_ACCURACY", null, null, false);
        assertEquals(PRIORITY_HIGH_ACCURACY, auditConfig.getLocationPriority());
        auditConfig = new AuditConfig("balanced", null, null, false);
        assertEquals(PRIORITY_BALANCED_POWER_ACCURACY, auditConfig.getLocationPriority());
        auditConfig = new AuditConfig("BALANCED", null, null, false);
        assertEquals(PRIORITY_BALANCED_POWER_ACCURACY, auditConfig.getLocationPriority());
        auditConfig = new AuditConfig("low_power", null, null, false);
        assertEquals(PRIORITY_LOW_POWER, auditConfig.getLocationPriority());
        auditConfig = new AuditConfig("low-power", null, null, false);
        assertEquals(PRIORITY_LOW_POWER, auditConfig.getLocationPriority());
        auditConfig = new AuditConfig("low_POWER", null, null, false);
        assertEquals(PRIORITY_LOW_POWER, auditConfig.getLocationPriority());
        auditConfig = new AuditConfig("no_power", null, null, false);
        assertEquals(PRIORITY_NO_POWER, auditConfig.getLocationPriority());
        auditConfig = new AuditConfig("no-power", null, null, false);
        assertEquals(PRIORITY_NO_POWER, auditConfig.getLocationPriority());
        auditConfig = new AuditConfig("NO_power", null, null, false);
        assertEquals(PRIORITY_NO_POWER, auditConfig.getLocationPriority());
        auditConfig = new AuditConfig("qwerty", null, null, false);
        assertEquals(PRIORITY_HIGH_ACCURACY, auditConfig.getLocationPriority());
        auditConfig = new AuditConfig("", null, null, false);
        assertEquals(PRIORITY_HIGH_ACCURACY, auditConfig.getLocationPriority());
        auditConfig = new AuditConfig(null, null, null, false);
        assertNull(auditConfig.getLocationPriority());
    }
}
