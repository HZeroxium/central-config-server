import {
  Dialog,
  DialogTitle,
  DialogContent,
  List,
  ListItem,
  ListItemText,
  Typography,
  Box,
  Divider,
  Chip,
} from "@mui/material";
import { getKeyDisplay } from "@hooks/useKeyboardShortcuts";

interface KeyboardShortcut {
  key: string;
  description: string;
  category: "navigation" | "action" | "interface";
}

/**
 * Keyboard shortcuts help dialog
 */
export function KeyboardShortcutsDialog({
  open,
  onClose,
}: {
  open: boolean;
  onClose: () => void;
}) {
  const shortcuts: KeyboardShortcut[] = [
    {
      key: "ctrl+k",
      description: "Open command palette",
      category: "navigation",
    },
    {
      key: "ctrl+b",
      description: "Toggle sidebar",
      category: "interface",
    },
    {
      key: "ctrl+/",
      description: "Show keyboard shortcuts",
      category: "interface",
    },
    {
      key: "ctrl+?",
      description: "Show keyboard shortcuts",
      category: "interface",
    },
    {
      key: "ctrl+r",
      description: "Refresh page data",
      category: "action",
    },
    {
      key: "escape",
      description: "Close modals and dialogs",
      category: "interface",
    },
  ];

  const shortcutsByCategory = shortcuts.reduce(
    (acc, shortcut) => {
      if (!acc[shortcut.category]) {
        acc[shortcut.category] = [];
      }
      acc[shortcut.category].push(shortcut);
      return acc;
    },
    {} as Record<string, KeyboardShortcut[]>
  );

  const categoryLabels: Record<string, string> = {
    navigation: "Navigation",
    action: "Actions",
    interface: "Interface",
  };

  return (
    <Dialog
      open={open}
      onClose={onClose}
      maxWidth="sm"
      fullWidth
      aria-labelledby="keyboard-shortcuts-dialog-title"
    >
      <DialogTitle id="keyboard-shortcuts-dialog-title">
        Keyboard Shortcuts
      </DialogTitle>
      <DialogContent>
        <Box sx={{ pb: 2 }}>
          {Object.entries(shortcutsByCategory).map(([category, items]) => (
            <Box key={category} sx={{ mb: 3 }}>
              <Typography
                variant="subtitle2"
                sx={{ fontWeight: 600, mb: 1, color: "text.secondary" }}
              >
                {categoryLabels[category]}
              </Typography>
              <List dense>
                {items.map((shortcut) => (
                  <ListItem
                    key={shortcut.key}
                    sx={{
                      px: 0,
                      py: 0.5,
                      display: "flex",
                      justifyContent: "space-between",
                      alignItems: "center",
                    }}
                  >
                    <ListItemText
                      primary={shortcut.description}
                      primaryTypographyProps={{
                        variant: "body2",
                      }}
                    />
                    <Chip
                      label={getKeyDisplay(shortcut.key)}
                      size="small"
                      sx={{
                        fontFamily: "monospace",
                        fontWeight: 500,
                        minWidth: 80,
                      }}
                    />
                  </ListItem>
                ))}
              </List>
              {category !== Object.keys(shortcutsByCategory).slice(-1)[0] && (
                <Divider sx={{ mt: 2 }} />
              )}
            </Box>
          ))}
        </Box>
        <Typography variant="caption" color="text.secondary">
          Press Esc to close
        </Typography>
      </DialogContent>
    </Dialog>
  );
}

export default KeyboardShortcutsDialog;

