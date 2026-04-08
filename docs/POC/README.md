# Pre-Award / Negotiations Review - Static Offline Version

This is a 100% static version of the Pre-Award / Negotiations Review application that runs entirely offline in your web browser.

## How to Run

**Simply double-click `index.html`** to open the application in your web browser.

That's it! No server, no Node.js, no npm, no internet connection required.

## Features

This application allows you to manage and review:

- **Safety Requirements Review** - Track safety compliance for research projects
- **Animal Research Review** - Manage animal research protocols and approvals
- **Human Research Review** - Handle human subjects research requirements
- **Acquisition/Contracting Review** - Review budget, personnel, equipment, and more

All data is automatically saved to your browser's localStorage, so your progress persists between sessions.

## Authentication

This offline version uses a simple demo authentication system:

- **Sign Up**: Create a local account (stored in browser)
- **Login**: Enter any email and password to access the app
- **Logout**: Clear your session

No actual authentication or server validation occurs - this is for local demo purposes only.

## Data Storage

All review data is stored locally in your browser using `localStorage`. This means:

- ✅ Your data persists even after closing the browser
- ✅ Works completely offline
- ✅ No server or database required
- ⚠️ Data is specific to your browser and computer
- ⚠️ Clearing browser data will delete all reviews

## Reset Functionality

Use the "Reset Checklist (Admin Only)" button at the bottom of the left panel to clear individual sections. This allows you to start fresh for any review section.

## Changes from the Node.js Version

### Removed Features:
- External database integration (replaced with localStorage)
- Real authentication system (replaced with demo auth)
- Server-side logic and API endpoints
- Multi-user support
- Network synchronization
- page2.html (unused secondary page)

### How Data is Handled:
- **Original**: Data stored in External PostgreSQL database
- **Static**: Data stored in browser's localStorage
- **Review IDs**: Hardcoded to 'TE020005'
- **User IDs**: Generated locally as 'user_' + timestamp

## Known Limitations

1. **Single User**: No multi-user support - only works for one user per browser
2. **No Backup**: Data exists only in your browser's localStorage
3. **No Sync**: Cannot share data between devices or browsers
4. **Browser Specific**: Data won't transfer if you switch browsers
5. **No Cloud Storage**: All data is local-only
6. **Limited to localStorage Size**: Typically ~5-10MB per domain

## Browser Compatibility

Works in all modern browsers that support:
- localStorage API
- ES6 JavaScript features
- Modern CSS

Tested in: Chrome, Firefox, Edge, Safari

## File Structure

```
/
├── index.html          # Main application page
├── styles.css          # All styling
├── script.js           # All application logic (localStorage-based)
├── public/
│   └── egs-banner.png  # Header banner image
└── README.md           # This file
```

## Technical Details

- **Framework**: Vanilla JavaScript (no frameworks)
- **Storage**: localStorage API
- **Styling**: Plain CSS
- **No Build Process**: Just HTML, CSS, and JavaScript

## Tips for Use

1. **Bookmark the file**: Create a bookmark to `file:///path/to/index.html` for quick access
2. **Regular Exports**: Since data is only in your browser, consider copying important information elsewhere periodically
3. **Same Browser**: Always use the same browser to access your data
4. **Don't Clear Data**: Avoid clearing browser data/cache if you want to keep your reviews

## Troubleshooting

**Q: My data disappeared!**
A: This happens if you cleared your browser data. localStorage data cannot be recovered once cleared.

**Q: Can I move this to another computer?**
A: Yes! Just copy the entire folder. However, the data won't transfer - only the application files.

**Q: The app won't open!**
A: Make sure you're opening index.html in a web browser. Some browsers may block localStorage for file:// URLs - try using Firefox or hosting the files on a local web server.

**Q: Can multiple people use this?**
A: Not simultaneously. Each browser instance has its own isolated storage. You would need separate browsers/computers for multiple users.

## Development Notes

This conversion maintained all UI and functionality from the original Node.js version while removing server dependencies:

- Authentication logic moved to browser-side with localStorage
- Database operations replaced with localStorage read/write
- All module imports consolidated into single script.js
- External SDK removed
- No build tools or npm packages required

---

**Version**: Static Offline 1.0
**Converted**: February 2026
**Original**: Node.js application
