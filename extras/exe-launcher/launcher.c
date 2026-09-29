/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) Andrea Antonello - https://g-ant.eu
 *
 * The HortonMachine is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 * Generic windows launcher for the HortonMachine apps.
 *
 * The exe runs the batch file named after itself, placed in the same folder,
 * without showing a console window: <folder>\<name>.exe -> <folder>\hm-<name>.bat
 *
 * The working directory is set to the exe folder, since the batch files use
 * relative paths. Command line arguments are passed on to the batch file.
 */
#define WIN32_LEAN_AND_MEAN
#include <windows.h>
#include <wchar.h>

#define PATH_SIZE 32768

static void showError( const wchar_t *title, const wchar_t *message ) {
    MessageBoxW(NULL, message, title, MB_OK | MB_ICONERROR);
}

int WINAPI wWinMain( HINSTANCE hInstance, HINSTANCE hPrevInstance, PWSTR args, int nCmdShow ) {
    static wchar_t exePath[PATH_SIZE];
    static wchar_t folder[PATH_SIZE];
    static wchar_t batPath[PATH_SIZE];
    static wchar_t comspec[PATH_SIZE];
    static wchar_t commandLine[3 * PATH_SIZE];
    static wchar_t message[2 * PATH_SIZE];

    DWORD length = GetModuleFileNameW(NULL, exePath, PATH_SIZE);
    if (length == 0 || length >= PATH_SIZE) {
        showError(L"HortonMachine launcher", L"Unable to get the path of the launcher.");
        return 1;
    }

    // split folder and name, removing the .exe extension
    wchar_t *lastSlash = wcsrchr(exePath, L'\\');
    wchar_t *name = lastSlash != NULL ? lastSlash + 1 : exePath;
    size_t folderLength = lastSlash != NULL ? (size_t) (lastSlash - exePath) : 0;
    wcsncpy(folder, exePath, folderLength);
    folder[folderLength] = L'\0';
    wchar_t *dot = wcsrchr(name, L'.');
    if (dot != NULL && _wcsicmp(dot, L".exe") == 0) {
        *dot = L'\0';
    }

    swprintf(batPath, PATH_SIZE, L"%ls\\hm-%ls.bat", folder, name);
    if (GetFileAttributesW(batPath) == INVALID_FILE_ATTRIBUTES) {
        swprintf(message, 2 * PATH_SIZE, L"The launcher script was not found:\n\n%ls", batPath);
        showError(L"HortonMachine launcher", message);
        return 1;
    }

    DWORD comspecLength = GetEnvironmentVariableW(L"ComSpec", comspec, PATH_SIZE);
    if (comspecLength == 0 || comspecLength >= PATH_SIZE) {
        wcscpy(comspec, L"cmd.exe");
    }

    // /s strips the outer quotes, so that paths with spaces are kept quoted: cmd /d /s /c ""script" args"
    swprintf(commandLine, 3 * PATH_SIZE, L"\"%ls\" /d /s /c \"\"%ls\" %ls\"", comspec, batPath, args != NULL ? args : L"");

    STARTUPINFOW startupInfo;
    PROCESS_INFORMATION processInfo;
    ZeroMemory(&startupInfo, sizeof(startupInfo));
    ZeroMemory(&processInfo, sizeof(processInfo));
    startupInfo.cb = sizeof(startupInfo);
    startupInfo.dwFlags = STARTF_USESHOWWINDOW;
    startupInfo.wShowWindow = SW_HIDE;

    if (!CreateProcessW(NULL, commandLine, NULL, NULL, FALSE, CREATE_NO_WINDOW, NULL, folder, &startupInfo, &processInfo)) {
        DWORD error = GetLastError();
        swprintf(message, 2 * PATH_SIZE, L"Unable to start (error %lu):\n\n%ls", (unsigned long) error, commandLine);
        showError(L"HortonMachine launcher", message);
        return 1;
    }
    CloseHandle(processInfo.hThread);
    CloseHandle(processInfo.hProcess);
    return 0;
}
