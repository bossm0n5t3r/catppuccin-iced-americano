#!/usr/bin/env bash
# Install the generated theme using Bash and standard system utilities.
set -euo pipefail

fail() {
  printf 'Error: %s\n' "$*" >&2
  exit 2
}

expand_home() {
  case "$1" in
  '~') printf '%s\n' "$HOME" ;;
  '~/'*) printf '%s/%s\n' "$HOME" "${1:2}" ;;
  *) printf '%s\n' "$1" ;;
  esac
}

# Resolve symlinks and dot segments even if the final file does not exist yet.
# Unlike realpath -m, this also works with the tools shipped with macOS.
resolve_path() {
  local remaining=$1 current=/ part link links=0
  [[ $remaining == /* ]] || remaining="$PWD/$remaining"
  while [[ -n $remaining ]]; do
    part=${remaining%%/*}
    if [[ $remaining == */* ]]; then remaining=${remaining#*/}; else remaining=; fi
    case "$part" in
    '' | .) continue ;;
    ..)
      current=${current%/*}
      current=${current:-/}
      continue
      ;;
    esac
    current="${current%/}/$part"
    if [[ -L $current ]]; then
      links=$((links + 1))
      [[ $links -le 40 ]] || fail "Too many symbolic links: $1"
      link=$(readlink "$current")
      if [[ $link == /* ]]; then current=/; else
        current=${current%/*}
        current=${current:-/}
      fi
      remaining="$link${remaining:+/$remaining}"
    fi
  done
  printf '%s\n' "$current"
}

config=
while [[ $# -gt 0 ]]; do
  case "$1" in
  --config)
    [[ $# -ge 2 && $2 != --* ]] || fail '--config requires a path'
    config=$2
    shift 2
    ;;
  --config=*)
    config=${1#*=}
    shift
    ;;
  -h | --help)
    printf 'Usage: %s [--config PATH]\n' "$0"
    exit 0
    ;;
  *) fail "Unknown option: $1" ;;
  esac
  [[ -n $config ]] || fail '--config requires a non-empty path'
done

script_directory=$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd -P)
source="$script_directory/iced-americano"
[[ -f $source ]] || fail "Missing generated theme: $source"
config_home=$(expand_home "${XDG_CONFIG_HOME:-$HOME/.config}")
config_directory="$config_home/ghostty"
target="$config_directory/themes/iced-americano"

if [[ -n $config ]]; then
  config=$(expand_home "$config")
else
  candidates=("$config_directory/config.ghostty" "$config_directory/config")
  if [[ $(uname -s) == Darwin ]]; then
    mac_directory="$HOME/Library/Application Support/com.mitchellh.ghostty"
    candidates+=("$mac_directory/config.ghostty" "$mac_directory/config")
  fi
  config=${candidates[0]}
  for candidate in "${candidates[@]}"; do
    if [[ -f $candidate ]]; then
      config=$candidate
      break
    fi
  done
fi

[[ ! -d $target || -L $target ]] || fail "Theme destination is a directory: $target"
resolved_config=$(resolve_path "$config")
resolved_source=$(resolve_path "$source")
resolved_target=$(resolve_path "$target")
if [[ $resolved_config == "$resolved_source" || $resolved_config == "$resolved_target" || $config -ef $source || $config -ef $target ]]; then
  fail 'Config must not be the theme file'
fi
if [[ ! -L $target && $resolved_target == "$resolved_source" ]]; then
  fail 'Theme destination must not be the source file'
fi
[[ ! -d $config ]] || fail "Config is a directory: $config"

original=
if [[ -e $config ]]; then
  # A sentinel prevents command substitution from stripping trailing newlines.
  original=$(cat -- "$config" && printf '\001')
  original=${original%$'\001'}
fi
# Normalize CRLF/CR on reads; do not rewrite an already-correct config.
original=${original//$'\r\n'/$'\n'}
original=${original//$'\r'/$'\n'}
remaining=$original
updated=
found=false
while [[ -n $remaining ]]; do
  if [[ $remaining == *$'\n'* ]]; then
    line=${remaining%%$'\n'*}
    remaining=${remaining#*$'\n'}
    ending=$'\n'
  else
    line=$remaining
    remaining=
    ending=
  fi
  if [[ $line =~ ^[[:space:]]*theme[[:space:]]*= ]]; then
    if [[ $found == false ]]; then updated+=$'theme = iced-americano\n'; fi
    found=true
  else
    updated+="$line$ending"
  fi
done
if [[ $found == false ]]; then
  if [[ -n $updated && $updated != *$'\n' ]]; then updated+=$'\n'; fi
  updated+=$'theme = iced-americano\n'
fi

mkdir -p -- "$(dirname -- "$target")"
if [[ -L $target || -e $target ]]; then rm -- "$target"; fi
ln -s -- "$source" "$target"
if [[ $updated != "$original" ]]; then
  mkdir -p -- "$(dirname -- "$config")"
  # Write through config symlinks rather than replacing them.
  printf '%s' "$updated" >"$config"
fi
printf 'Theme: %s -> %s\n' "$target" "$source"
printf 'Config: %s (theme = iced-americano)\n' "$config"
