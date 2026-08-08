/**
 * Hashcat Formatter - Build hashcat 22000 Records
 *
 * Formats captured PMKID and EAPOL data into hashcat-compatible records.
 * Mode 22000: WPA-PBKDF2-PMKID+EAPOL
 */

#ifndef HASHCAT_FORMATTER_H
#define HASHCAT_FORMATTER_H

#include <stdint.h>
#include <stddef.h>

/**
 * Format a PMKID capture into hashcat 22000 record.
 *
 * Output format: WPA*02*PMKID*MAC_AP*MAC_STA*ESSID_HEX*00
 *
 * @param bssid AP MAC address (6 bytes)
 * @param client_mac Client MAC address (6 bytes)
 * @param pmkid PMKID value (16 bytes)
 * @param ssid Network SSID
 * @param ssid_len SSID length
 * @param output Buffer for output string
 * @param output_size Size of output buffer
 * @return Length of output string, or 0 on error
 */
size_t hashcat_format_pmkid(
    const uint8_t *bssid,
    const uint8_t *client_mac,
    const uint8_t *pmkid,
    const char *ssid,
    uint8_t ssid_len,
    char *output,
    size_t output_size
);

/**
 * Format an EAPOL capture into hashcat 22000 record.
 *
 * Output format: WPA*01*MIC*MAC_AP*MAC_STA*ESSID_HEX*NONCE_AP*EAPOL_CLIENT*MESSAGE_PAIR
 *
 * @param bssid AP MAC address (6 bytes)
 * @param client_mac Client MAC address (6 bytes)
 * @param anonce AP nonce from M1 (32 bytes)
 * @param eapol_frame Complete EAPOL frame from M2 (with MIC zeroed)
 * @param eapol_len Length of EAPOL frame
 * @param mic MIC from M2 (16 bytes)
 * @param ssid Network SSID
 * @param ssid_len SSID length
 * @param output Buffer for output string
 * @param output_size Size of output buffer
 * @return Length of output string, or 0 on error
 */
size_t hashcat_format_eapol(
    const uint8_t *bssid,
    const uint8_t *client_mac,
    const uint8_t *anonce,
    const uint8_t *eapol_frame,
    size_t eapol_len,
    const uint8_t *mic,
    const char *ssid,
    uint8_t ssid_len,
    char *output,
    size_t output_size
);

#endif // HASHCAT_FORMATTER_H
